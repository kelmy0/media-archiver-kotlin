package com.kelmy.mediaarchiver.infrastructure.system

import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.util.Collections
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

data class CommandResult(
    val exitCode: Int,
    val stdoutLines: List<String>,
    val stderr: String
)


object CommandRunner {
    private val log = LoggerFactory.getLogger(CommandRunner::class.java)

    /**
     * @param timeoutSeconds maximum total execution time (null = no limit)
     * @param idleTimeoutSeconds maximum time without any output (stdout or stderr) (null = disabled)
     */
    @JvmStatic
    @JvmOverloads
    fun run(
        command: List<String>,
        timeoutSeconds: Long? = 120,
        idleTimeoutSeconds: Long? = null,
        onStdoutLine: (String) -> Unit = {}
    ): CommandResult {
        if (command.isEmpty()) {
            return CommandResult(exitCode = -1, stdoutLines = emptyList(), stderr = "")
        }

        val process = ProcessBuilder(command).start()
        process.outputStream.close()

        val startNs = System.nanoTime()
        val lastActivityNs = AtomicLong(startNs)
        val stdoutLines = Collections.synchronizedList(mutableListOf<String>())
        val stderrLines = Collections.synchronizedList(mutableListOf<String>())

        val stdoutThread = startReader("cmd-stdout", process.inputStream, lastActivityNs) { line ->
            stdoutLines.add(line)
            onStdoutLine(line)
        }
        val stderrThread = startReader("cmd-stderr", process.errorStream, lastActivityNs) { line ->
            stderrLines.add(line)
        }

        try {
            while (!process.waitFor(1, TimeUnit.SECONDS)) {
                val now = System.nanoTime()

                if (timeoutSeconds != null &&
                    now - startNs > TimeUnit.SECONDS.toNanos(timeoutSeconds)
                ) {
                    kill(process)
                    throw RuntimeException(
                        "The command exceeded the time limit of ${timeoutSeconds}s: ${command.joinToString(" ")}"
                    )
                }

                if (idleTimeoutSeconds != null &&
                    now - lastActivityNs.get() > TimeUnit.SECONDS.toNanos(idleTimeoutSeconds)
                ) {
                    kill(process)
                    throw RuntimeException(
                        "The command produced no output for ${idleTimeoutSeconds}s: ${command.joinToString(" ")}"
                    )
                }
            }
        } finally {
            if (process.isAlive) kill(process)
        }

        stdoutThread.join(5_000)
        stderrThread.join(5_000)
        if (stdoutThread.isAlive || stderrThread.isAlive) {
            log.warn("Output streams were not closed after process exit; output may be incomplete")
        }

        val errorText = synchronized(stderrLines) { stderrLines.joinToString("\n") }
        if (errorText.isNotBlank()) {
            log.warn("Process stderr:\n$errorText")
        }

        return CommandResult(
            exitCode = process.exitValue(),
            stdoutLines = synchronized(stdoutLines) { stdoutLines.toList() },
            stderr = errorText
        )
    }

    private fun startReader(
        name: String,
        stream: InputStream,
        lastActivityNs: AtomicLong,
        onLine: (String) -> Unit
    ): Thread = thread(name = name, isDaemon = true) {
        try {
            stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines.forEach { line ->
                    lastActivityNs.set(System.nanoTime())
                    try {
                        onLine(line)
                    } catch (e: Exception) {
                        log.warn("Error while handling output line", e)
                    }
                }
            }
        } catch (e: IOException) {
            log.debug("Stream closed: {}", e.message)
        }
    }

    private fun kill(process: Process) {
        process.descendants().forEach { it.destroyForcibly() }
        process.destroyForcibly()
        process.waitFor(5, TimeUnit.SECONDS)
    }
}