package com.kelmy.mediaarchiver.presentation.gui

import com.kelmy.mediaarchiver.archiving.application.ArchivingProgressListener
import com.kelmy.mediaarchiver.library.domain.model.Channel
import com.kelmy.mediaarchiver.presentation.AppServices
import com.kelmy.mediaarchiver.presentation.ApplicationView
import com.kelmy.mediaarchiver.presentation.UiFactory
import org.slf4j.LoggerFactory
import javax.swing.SwingUtilities

/**
 * Concrete factory of the **graphical** (Swing) interface.
 */
internal class GuiUiFactory : UiFactory {
    override fun createProgressListener(): ArchivingProgressListener = GuiArchivingProgressListener()

    override fun createApplicationView(services: AppServices): ApplicationView = GuiApplicationView(services)
}

/**
 * [ApplicationView] that opens the [MainWindow] on the Swing thread and returns immediately.
 */
internal class GuiApplicationView(private val services: AppServices) : ApplicationView {
    override fun show() {
        SwingUtilities.invokeLater { MainWindow(services).isVisible = true }
    }
}

/**
 * [ArchivingProgressListener] of the graphical interface.
 *
 * It reports through the logger: [GuiLogAppender] already forwards every log line to the
 * "Activity Log" tab, so the window needs no direct reference to the application layer.
 */
internal class GuiArchivingProgressListener : ArchivingProgressListener {
    private companion object {
        val log = LoggerFactory.getLogger(GuiArchivingProgressListener::class.java)
    }

    override fun onRoutineStarted() = log.info("=== Archiving routine started ===")

    override fun onChannelStarted(position: Int, total: Int, channel: Channel) =
        log.info("[{}/{}] Processing channel '{}' ({})", position, total, channel.name, channel.platform)

    override fun onRoutineFinished() = log.info("=== Archiving routine finished ===")
}
