package com.kelmy.mediaarchiver.archiving.application

import com.kelmy.mediaarchiver.archiving.domain.model.MediaItem
import com.kelmy.mediaarchiver.library.domain.port.VideoRepository
import com.kelmy.mediaarchiver.archiving.domain.port.Extractor
import org.slf4j.LoggerFactory

/**
 * Default [MediaCollector]: parses and de-duplicates scan output, then filters it with the registered videos.
 *
 * @property videoRepository videos already registered, used to detect items that were already downloaded.
 */
internal class DefaultMediaCollector(
    private val videoRepository: VideoRepository
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

        val alreadyDownloaded = videoRepository.findExistingIds(platform, items.keys)

        for (id in alreadyDownloaded) {
            items.remove(id)
            log.info("Skipping already downloaded item [platform=$platform, id=$id]")
        }

        return alreadyDownloaded.size
    }
}
