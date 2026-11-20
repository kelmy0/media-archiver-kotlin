package com.kelmy.mediaarchiver.utils

import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

data class CommandResult(
    val exitCode: Int,
    val stdoutLines: List<String>,
    val stderr: String
)


object CommandRunner {
    private val log = LoggerFactory.getLogger(CommandRunner::class.java)

    @JvmStatic
    fun run(
        command: List<String>,
        timeoutSeconds: Long = 120,
        onStdoutLine: (String) -> Unit = {}
    ): CommandResult {
        if (command.isEmpty()) {
            return CommandResult(exitCode = -1, stdoutLines = emptyList(), stderr = "")
        }

        val process = ProcessBuilder(command).start()
        val stdoutLines = mutableListOf<String>()

        process.inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.forEach { line ->
                stdoutLines.add(line)
                onStdoutLine(line)
            }
        }

        val errorText = process.errorStream.bufferedReader(Charsets.UTF_8).readText()
        if (errorText.isNotBlank()) {
            log.warn("Process stderr:\n$errorText")
        }

        val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            throw RuntimeException("The command exceeded the time limit of ${timeoutSeconds}s: ${command.joinToString(" ")}")
        }

        return CommandResult(
            exitCode = process.exitValue(),
            stdoutLines = stdoutLines,
            stderr = errorText
        )
    }
}