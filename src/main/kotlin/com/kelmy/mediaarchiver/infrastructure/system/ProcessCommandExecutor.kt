package com.kelmy.mediaarchiver.infrastructure.system

import com.kelmy.mediaarchiver.core.domain.port.CommandExecutor
import com.kelmy.mediaarchiver.core.domain.port.CommandResult
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.util.Collections
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

/**
 * [CommandExecutor] that starts real operating-system processes with [ProcessBuilder].
 *
 * stdout and stderr are drained by two daemon threads, so a chatty process can never block on a
 * full pipe, and the calling thread only waits for the process to finish (checking the timeouts
 * once per second).
 */
class ProcessCommandExecutor : CommandExecutor {
    private val log = LoggerFactory.getLogger(ProcessCommandExecutor::class.java)

    /**
     * Starts the process and waits for it. When a timeout is exceeded, the process **and its
     * descendants** are killed and a [RuntimeException] is thrown.
     *
     * An empty [command] is not an error: it returns exit code `-1` with empty output.
     */
    override fun execute(
        command: List<String>,
        timeoutSeconds: Long?,
        idleTimeoutSeconds: Long?,
        captureStdout: Boolean,
        onStdoutLine: (String) -> Unit
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
            if (captureStdout) stdoutLines.add(line)
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

    /**
     * Starts a daemon thread that reads [stream] line by line (UTF-8), refreshes [lastActivityNs]
     * on each line and hands the line to [onLine]. An exception thrown by [onLine] is logged and
     * does not stop the reading.
     */
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

    /** Forcibly kills [process] and its descendants, waiting up to 5 seconds for it to die. */
    private fun kill(process: Process) {
        process.descendants().forEach { it.destroyForcibly() }
        process.destroyForcibly()
        process.waitFor(5, TimeUnit.SECONDS)
    }
}
