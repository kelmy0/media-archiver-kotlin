package com.kelmy.mediaarchiver.core


import com.kelmy.mediaarchiver.extractors.ExtractorProvider
import com.kelmy.mediaarchiver.model.ChannelModel
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

class ChannelProcessor(
    private val extractors: ExtractorProvider
) : ChannelProcessorProvider {
    companion object {
        private val log = LoggerFactory.getLogger(ChannelProcessor::class.java)
    }

    override fun processChanel(
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

        // 2. get endpoints
        val endpoints = extractor.getEndpointsToScan(channel)

        // 3. get contents
        for (endpoint in endpoints) {
            val lightScanCommand = extractor.getLightScanCommand(endpoint)
        }

        return false
    }
}
