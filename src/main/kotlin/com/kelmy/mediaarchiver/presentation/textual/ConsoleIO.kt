package com.kelmy.mediaarchiver.presentation.textual

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintStream

/**
 * Small wrapper around the console (standard input and output), so the menus do not repeat the
 * "print a prompt, read a line, handle end of input" code.
 *
 * The streams are constructor parameters so a test can feed it a script instead of a keyboard.
 *
 * @property reader where the user's answers are read from.
 * @property out    where messages are printed to.
 */
internal class ConsoleIO(
    private val reader: BufferedReader = BufferedReader(InputStreamReader(System.`in`)),
    private val out: PrintStream = System.out
) {
    /** Becomes `true` when the input is closed (Ctrl+D / Ctrl+Z); menus use it to stop looping. */
    var endOfInput: Boolean = false
        private set

    /** Prints [message] followed by a line break. */
    fun println(message: String = "") = out.println(message)

    /**
     * Prints [prompt], waits for a line and returns it trimmed.
     * At the end of the input it returns an empty string and sets [endOfInput].
     */
    fun ask(prompt: String): String {
        out.print("$prompt ")
        out.flush()

        val line = reader.readLine()
        if (line == null) {
            endOfInput = true
            return ""
        }
        return line.trim()
    }

    /** Like [ask], but returns the answer as a number, or `null` (after telling the user) if it is blank or not a number. */
    fun askInt(prompt: String): Int? {
        val raw = ask(prompt)
        if (raw.isEmpty()) return null
        return raw.toIntOrNull() ?: run {
            println("'$raw' is not a number.")
            null
        }
    }

    /** Asks a yes/no question; only `y` (any case) means yes. */
    fun confirm(prompt: String): Boolean = ask("$prompt (y/n):").equals("y", ignoreCase = true)
}
