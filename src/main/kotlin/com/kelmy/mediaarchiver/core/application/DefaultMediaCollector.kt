package com.kelmy.mediaarchiver.core.application

import com.kelmy.mediaarchiver.core.domain.model.MediaItem
import com.kelmy.mediaarchiver.core.domain.port.DownloadHistoryRepository
import com.kelmy.mediaarchiver.core.domain.port.Extractor
import org.slf4j.LoggerFactory

/**
 * Default [MediaCollector]: parses and de-duplicates scan output, then filters it with the download history.
 *
 * @property downloadHistoryRepository history used to detect items that were already downloaded.
 */
class DefaultMediaCollector(
    private val downloadHistoryRepository: DownloadHistoryRepository
) : MediaCollector {
    companion object {
        private val log = LoggerFactory.getLogger(DefaultMediaCollector::class.java)
    }

    override fun collectFromLine(
        rawLine: String,
        platform: String,
        extractor: Extractor,
        targetItems: MutableMap<String, MediaItem>
    ): Boolean {
        if (rawLine.isBlank()) return false

        val item = try {
            extractor.parseItem(rawLine)
        } catch (e: Exception) {
            log.warn("Skipping invalid/unparseable item for platform '$platform': ${e.message}")
            return false
        }

        if (targetItems.containsKey(item.id)) {
            log.info("Skipping duplicate item in current scan [platform=$platform, id=${item.id}]")
            return false
        }

        targetItems[item.id] = item
        return true
    }

    override fun removeAlreadyDownloaded(platform: String, items: MutableMap<String, MediaItem>): Int {
        if (items.isEmpty()) return 0

        val alreadyDownloaded = downloadHistoryRepository.findExistingIds(platform, items.keys)

        for (id in alreadyDownloaded) {
            items.remove(id)
            log.info("Skipping already downloaded item [platform=$platform, id=$id]")
        }

        return alreadyDownloaded.size
    }
}
