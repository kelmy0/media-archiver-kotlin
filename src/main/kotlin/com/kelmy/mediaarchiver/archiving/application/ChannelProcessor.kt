package com.kelmy.mediaarchiver.archiving.application

import com.kelmy.mediaarchiver.library.domain.model.Channel
import java.time.Instant

/**
 * Use case that archives **one** channel: scans it, filters what is new and downloads it.
 */
interface ChannelProcessor {
    /**
     * Processes a single channel.
     *
     * The channel is skipped while it is still in cooldown, when no extractor exists for its
     * platform, or when the scan finds nothing new.
     *
     * @param channel         the channel to process.
     * @param rootDownloadDir root download directory; the channel's folder is created inside it.
     * @param cooldownMinutes configured cooldown, in minutes.
     * @param now             reference moment used to evaluate the cooldown.
     * @return `true` if there was something new to download (even if some downloads failed),
     *         `false` if the channel was skipped or nothing new was found.
     */
    fun processChannel(
        channel: Channel, rootDownloadDir: String, cooldownMinutes: Int, now: Instant
    ): Boolean
}
