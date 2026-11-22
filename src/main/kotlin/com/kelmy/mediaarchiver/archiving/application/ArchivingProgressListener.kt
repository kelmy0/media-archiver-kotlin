package com.kelmy.mediaarchiver.archiving.application

import com.kelmy.mediaarchiver.library.domain.model.Channel

/**
 * Observer notified about the progress of an archiving routine.
 *
 * It lets the application layer report progress **without knowing how it is displayed**
 * (colored console banners, GUI, logs...). Each presentation layer provides its own
 * implementation. All methods have an empty default body, so implementers only override
 * what they need.
 */
interface ArchivingProgressListener {
    /** Called once, before any channel is processed. */
    fun onRoutineStarted() {}

    /**
     * Called right before a channel starts being processed.
     *
     * @param position 1-based position of the channel in this run.
     * @param total    number of channels in this run.
     * @param channel  the channel about to be processed.
     */
    fun onChannelStarted(position: Int, total: Int, channel: Channel) {}

    /** Called once, after every channel has been handled (also when there was nothing to do). */
    fun onRoutineFinished() {}

    companion object {
        /** Listener that ignores every notification. */
        val NONE: ArchivingProgressListener = object : ArchivingProgressListener {}
    }
}
