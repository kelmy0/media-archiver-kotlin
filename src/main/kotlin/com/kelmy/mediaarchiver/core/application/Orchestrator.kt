package com.kelmy.mediaarchiver.core.application

import com.kelmy.mediaarchiver.core.domain.model.Channel
import com.kelmy.mediaarchiver.core.domain.port.ChannelsRepository
import com.kelmy.mediaarchiver.core.domain.port.SettingsStore
import java.time.Instant

/**
 * Entry point of the archiving use case: runs one full archiving routine over all active channels.
 *
 * For each active channel it delegates the work to a [ChannelProcessor]; at the end it records the
 * run time as `lastChecked` **only for the channels that were actually processed**. Channels that
 * were skipped (cooldown, unknown platform, nothing new) keep their previous `lastChecked`.
 *
 * @property settingsStore     source of the download path and of the cooldown.
 * @property channelProcessor  does the actual work for a single channel.
 * @property channelsRepository source of the active channels and destination of `lastChecked`.
 * @property progressListener  receives progress notifications. Defaults to no-op.
 */
class Orchestrator(
    private val settingsStore: SettingsStore,
    private val channelProcessor: ChannelProcessor,
    private val channelsRepository: ChannelsRepository,
    private val progressListener: ArchivingProgressListener = ArchivingProgressListener.NONE
) {

    /**
     * Runs the routine synchronously. It blocks until every channel is processed,
     * so callers that must stay responsive (like the GUI) should call it from a background thread.
     */
    fun run() {
        progressListener.onRoutineStarted()
        processActiveChannels()
        progressListener.onRoutineFinished()
    }

    private fun processActiveChannels() {
        val settings = settingsStore.current()
        val channels = channelsRepository.getActiveChannels()

        if (channels.isEmpty()) {
            return
        }

        val downloadPath = settings.path.downloadPath
        val cooldown = settings.cooldownMinutes
        val now: Instant = Instant.now()
        val processedChannels = ArrayList<Channel>()

        for ((index, channel) in channels.withIndex()) {
            progressListener.onChannelStarted(index + 1, channels.size, channel)

            val processedSuccessfully = channelProcessor.processChannel(channel, downloadPath, cooldown, now)

            if (processedSuccessfully) {
                processedChannels.add(channel)
            }
        }

        channelsRepository.updateLastChecked(processedChannels, now)
    }
}
