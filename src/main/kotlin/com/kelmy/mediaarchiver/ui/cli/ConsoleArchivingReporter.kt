package com.kelmy.mediaarchiver.ui.cli

import com.kelmy.mediaarchiver.core.application.ArchivingProgressListener
import com.kelmy.mediaarchiver.core.domain.model.Channel

/**
 * [ArchivingProgressListener] that prints colored banners to the standard output.
 *
 * It replaces the `println` calls that used to live inside the application layer, which
 * should not know that a terminal exists.
 */
class ConsoleArchivingReporter : ArchivingProgressListener {
    private companion object {
        const val ROUTINE_LINE = "=============================="
        const val CHANNEL_LINE = "------------------------------------------------"
    }

    override fun onRoutineStarted() = printRoutineBanner("STARTING ARCHIVING ROUTINE")

    override fun onChannelStarted(position: Int, total: Int, channel: Channel) {
        println("${AnsiColors.CYAN}$CHANNEL_LINE${AnsiColors.RESET}")
        println("${AnsiColors.BOLD}[$position/$total]  Processing: ${channel.name} (Platform: ${channel.platform})${AnsiColors.RESET}")
        println("${AnsiColors.CYAN}$CHANNEL_LINE${AnsiColors.RESET}")
    }

    override fun onRoutineFinished() = printRoutineBanner("ARCHIVING ROUTINE COMPLETED")

    /** Prints [title] between two cyan separator lines. */
    private fun printRoutineBanner(title: String) {
        println("${AnsiColors.CYAN}$ROUTINE_LINE${AnsiColors.RESET}")
        println("${AnsiColors.BOLD}$title${AnsiColors.RESET}")
        println("${AnsiColors.CYAN}$ROUTINE_LINE${AnsiColors.RESET}")
    }
}
