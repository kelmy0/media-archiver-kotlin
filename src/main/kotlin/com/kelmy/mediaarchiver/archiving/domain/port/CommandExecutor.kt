package com.kelmy.mediaarchiver.archiving.domain.port

/**
 * Outcome of an external command.
 *
 * @property exitCode    Process exit code (`-1` when the command list was empty).
 * @property stdoutLines Lines written to standard output. Empty when the command was executed
 *                       with `captureStdout = false`.
 * @property stderr      Everything written to standard error, joined by line breaks.
 */
data class CommandResult(
    val exitCode: Int,
    val stdoutLines: List<String>,
    val stderr: String
)

/**
 * Port for running external programs (`yt-dlp`, `ffmpeg`...).
 *
 * It exists so the application layer does not depend on `ProcessBuilder`, and so tests can
 * replace the real processes with a fake implementation.
 */
interface CommandExecutor {
    /**
     * Runs [command] and waits for it to finish.
     *
     * @param command            program and arguments (no shell is involved).
     * @param timeoutSeconds     maximum total execution time, or `null` for no limit.
     * @param idleTimeoutSeconds maximum time without any output (stdout or stderr), or `null` to disable.
     * @param captureStdout      when `true`, every stdout line is also kept in [CommandResult.stdoutLines].
     *                           Pass `false` for long outputs that are fully handled by [onStdoutLine],
     *                           so they are not kept in memory.
     * @param onStdoutLine       called for each stdout line, **from a background thread**.
     * @return the command result.
     * @throws RuntimeException if a timeout is exceeded (the process is killed) or the process cannot be started.
     */
    fun execute(
        command: List<String>,
        timeoutSeconds: Long? = 120,
        idleTimeoutSeconds: Long? = null,
        captureStdout: Boolean = true,
        onStdoutLine: (String) -> Unit = {}
    ): CommandResult
}
