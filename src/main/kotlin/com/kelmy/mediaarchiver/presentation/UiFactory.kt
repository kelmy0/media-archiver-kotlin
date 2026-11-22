package com.kelmy.mediaarchiver.presentation

import com.kelmy.mediaarchiver.archiving.application.ArchivingProgressListener

/**
 * A screen (or a whole interface) that the user interacts with.
 */
interface ApplicationView {
    /**
     * Shows the interface. The textual implementation blocks until the user leaves;
     * the graphical one returns right after opening its window.
     */
    fun show()
}

/**
 * **Abstract Factory** of the user interface.
 *
 * It creates a *family* of products that must belong to the same kind of interface: the main
 * [ApplicationView] and the listener that reports the archiving progress. The rest of the application only
 * talks to this interface, so switching from console to windows is just a different concrete factory
 * (see [UiFactoryProvider]); no other code changes.
 *
 * Implementations: `TextualUiFactory` (console) and `GuiUiFactory` (Swing).
 */
interface UiFactory {
    /** Creates the listener that shows the progress of an archiving routine in this kind of interface. */
    fun createProgressListener(): ArchivingProgressListener

    /** Creates the main view, wired to the given [services]. */
    fun createApplicationView(services: AppServices): ApplicationView
}
