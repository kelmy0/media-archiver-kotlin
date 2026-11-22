package com.kelmy.mediaarchiver.presentation.textual

import com.kelmy.mediaarchiver.archiving.application.ArchivingProgressListener
import com.kelmy.mediaarchiver.presentation.AppServices
import com.kelmy.mediaarchiver.presentation.ApplicationView
import com.kelmy.mediaarchiver.presentation.UiFactory

/**
 * Concrete factory of the **console** interface: menus read from the keyboard and colored banners
 * printed to the terminal.
 */
internal class TextualUiFactory : UiFactory {
    override fun createProgressListener(): ArchivingProgressListener = ConsoleArchivingReporter()

    override fun createApplicationView(services: AppServices): ApplicationView = TextualApplicationView(services)
}
