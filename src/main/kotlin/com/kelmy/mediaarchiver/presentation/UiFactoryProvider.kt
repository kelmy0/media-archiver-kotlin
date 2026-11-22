package com.kelmy.mediaarchiver.presentation

import com.kelmy.mediaarchiver.presentation.gui.GuiUiFactory
import com.kelmy.mediaarchiver.presentation.textual.TextualUiFactory

/** The kinds of user interface the application can run with. */
enum class UiMode { TEXTUAL, GUI }

/**
 * Static class that holds the **static configuration** of the abstract factory: which [UiMode] is used
 * and which concrete [UiFactory] belongs to each mode.
 *
 * The mode is [DEFAULT_MODE] unless the command line says otherwise (`--cli` or `--gui`), so the
 * program can run with either interface without touching `Main` or any other code.
 */
object UiFactoryProvider {
    /** Interface used when no argument chooses one. Change this single line to change the default. */
    private val DEFAULT_MODE = UiMode.GUI

    /** Returns the mode requested by [args] (`--cli` -> [UiMode.TEXTUAL], `--gui` -> [UiMode.GUI]), or the default. */
    @JvmStatic
    fun modeFrom(args: Array<String>): UiMode = when {
        "--cli" in args -> UiMode.TEXTUAL
        "--gui" in args -> UiMode.GUI
        else -> DEFAULT_MODE
    }

    /** Returns the concrete factory of [mode]. */
    @JvmStatic
    fun create(mode: UiMode): UiFactory = when (mode) {
        UiMode.TEXTUAL -> TextualUiFactory()
        UiMode.GUI -> GuiUiFactory()
    }
}
