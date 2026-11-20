package com.kelmy.mediaarchiver.core

import com.kelmy.mediaarchiver.extractors.Extractor
import com.kelmy.mediaarchiver.extractors.ExtractorProvider
import com.kelmy.mediaarchiver.model.ChannelModel
import com.kelmy.mediaarchiver.model.MediaItemModel
import com.kelmy.mediaarchiver.repositories.DownloadHistoryRepositoryProvider
import com.kelmy.mediaarchiver.utils.CommandRunner
import org.slf4j.LoggerFactory
import java.time.Instant

class ChannelProcessor(
    private val extractors: ExtractorProvider,
    private val mediaCollectorProvider: MediaCollectorProvider
) : ChannelProcessorProvider {
    companion object {
        private val log = LoggerFactory.getLogger(ChannelProcessor::class.java)
    }

    override fun processChannel(
        channel: ChannelModel, rootDownloadDir: String, cooldownMinutes: Int, now: Instant
    ): Boolean {
        val remaining = channel.getRemainingCooldown(now, cooldownMinutes)
        if (!remaining.isZero) {
            log.info("Channel: ${channel.name} is on cooldown. Remaining: ${remaining.toMinutes()}m ${remaining.toSecondsPart()}s")
            return false
        }

        // 1. get the channel extractor
        val extractor = try {
            this.extractors.get(channel.platform)
        } catch (e: IllegalArgumentException) {
            log.error("Failed to process channel '${channel.name}': ${e.message}")
            return false
        }

        log.info("Starting processing for channel: ${channel.name} using extractor '${extractor.name}'...")

        // 2. get endpoints for this channel
        val endpoints = extractor.getEndpointsToScan(channel)
        val newItems = LinkedHashMap<String, MediaItemModel>()

        // 3. get contents
        for (endpoint in endpoints) {
            val lightScanCommand = extractor.getLightScanCommand(endpoint)

            try {
                val result = CommandRunner.run(lightScanCommand) { line ->
                    mediaCollectorProvider.collectFromLine(
                        rawLine = line,
                        platform = channel.platform,
                        extractor = extractor,
                        targetNewItems = newItems
                    )
                }

                if (result.exitCode != 0) {
                    log.warn("yt-dlp exited with code ${result.exitCode} for the endpoint: $endpoint")
                }
            } catch (e: Exception) {
                log.error("Failed to execute command for $endpoint: ${e.message}", e)
            }
        }

        println(newItems)

        return false
    }
}
