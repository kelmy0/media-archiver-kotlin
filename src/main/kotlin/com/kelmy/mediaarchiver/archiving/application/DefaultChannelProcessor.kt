package com.kelmy.mediaarchiver.archiving.application

import com.kelmy.mediaarchiver.library.domain.model.Channel
import com.kelmy.mediaarchiver.archiving.domain.model.MediaItem
import com.kelmy.mediaarchiver.archiving.domain.port.CommandExecutor
import com.kelmy.mediaarchiver.archiving.domain.port.ExtractorRegistry
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

/**
 * Default [ChannelProcessor].
 *
 * Flow: check cooldown -> find extractor -> scan every endpoint (parsing and de-duplicating items)
 * -> drop the items already registered in the database (one batched query) -> for the ones still
 * unknown, ask the folder's `archive.txt` (fallback, once per folder) -> create the channel folder
 * -> download each new item sequentially.
 *
 * @property extractors      registry used to find the extractor of the channel's platform.
 * @property mediaCollector  filters the scan output down to new items.
 * @property mediaDownloader downloads each new item.
 * @property archiveFileFallback second lookup (the folder's `archive.txt`) for items the database does not know.
 * @property commandExecutor runs the light scan command.
 */
internal class DefaultChannelProcessor(
    private val extractors: ExtractorRegistry,
    private val mediaCollector: MediaCollector,
    private val mediaDownloader: MediaDownloader,
    private val archiveFileFallback: ArchiveFileFallback,
    private val commandExecutor: CommandExecutor
) : ChannelProcessor {
    companion object {
        private val log = LoggerFactory.getLogger(DefaultChannelProcessor::class.java)

        /** Maximum duration of one light scan before the process is killed. */
        private const val SCAN_TIMEOUT_SECONDS = 30L
    }

    override fun processChannel(
        channel: Channel, rootDownloadDir: String, cooldownMinutes: Int, now: Instant
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
        val newItems = LinkedHashMap<String, MediaItem>()

        // 3. get contents and remove duplicated (while scanning, `newItems` holds every scanned item)
        for (endpoint in endpoints) {
            val lightScanCommand = extractor.getLightScanCommand(endpoint)

            try {
                // Every line is consumed by the callback, so the executor must not keep them
                // all in memory too (captureStdout = false).
                val result = commandExecutor.execute(
                    command = lightScanCommand,
                    timeoutSeconds = SCAN_TIMEOUT_SECONDS,
                    idleTimeoutSeconds = null,
                    captureStdout = false,
                ) { line ->
                    mediaCollector.collectFromLine(
                        rawLine = line,
                        platform = channel.platform,
                        extractor = extractor,
                        targetItems = newItems
                    )
                }

                if (result.exitCode != 0) {
                    log.warn("yt-dlp exited with code ${result.exitCode} for the endpoint: $endpoint")
                }
            } catch (e: Exception) {
                log.error("Failed to execute command for $endpoint: ${e.message}", e)
            }
        }

        val channelDir = Path.of(rootDownloadDir).resolve(channel.folderName)

        // 4. drop what was already downloaded, with a single batched query; from here on `newItems` really is new.
        try {
            mediaCollector.removeAlreadyDownloaded(channel.platform, newItems)

            // 4b. fallback: whatever the database does not know may still be in the folder's archive.txt
            archiveFileFallback.registerKnownItems(channel, channelDir, newItems)
        } catch (e: Exception) {
            log.error("Could not check the registered videos for channel '${channel.name}': ${e.message}", e)
            return false
        }

        if (newItems.isEmpty()) {
            log.info("No new items found for channel: ${channel.name}")
            return false
        }

        Files.createDirectories(channelDir)
        val archivePath = channelDir.resolve("archive.txt")

        val itemsToDownload = newItems.values.toList()
        val totalItems = itemsToDownload.size
        var successCount = 0
        for ((index, item) in itemsToDownload.withIndex()) {
            val currentNumber = index + 1
            val remainingAfterCurrent = totalItems - currentNumber

            log.info(
                "[Channel: ${channel.name}] Downloading [$currentNumber/$totalItems] ($remainingAfterCurrent remaining): " +
                        "'${item.title}' (id=${item.id})"
            )

            val downloaded = mediaDownloader.downloadAndRecord(
                item, channel.platform, channel.id, extractor, channelDir, archivePath, null
            )

            if (downloaded) {
                successCount++
                log.info(
                    "[Channel: ${channel.name}] Completed [$currentNumber/$totalItems] ($remainingAfterCurrent remaining): id=${item.id}"
                )
            } else {
                log.warn(
                    "[Channel: ${channel.name}] Failed [$currentNumber/$totalItems] ($remainingAfterCurrent remaining): id=${item.id}"
                )
            }
        }

        log.info(
            "[Channel: ${channel.name}] Finished downloads: $successCount/$totalItems succeeded."
        )
        return true
    }
}
