package com.kelmy.mediaarchiver.core.application

import com.kelmy.mediaarchiver.core.domain.port.Extractor
import com.kelmy.mediaarchiver.core.domain.model.MediaItem
import com.kelmy.mediaarchiver.core.domain.port.DownloadHistoryRepository
import org.slf4j.LoggerFactory

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
        targetNewItems: MutableMap<String, MediaItem>
    ): Boolean {
        if (rawLine.isBlank()) return false

        val item = try {
            extractor.parseItem(rawLine)
        } catch (e: Exception) {
            log.warn("Skipping invalid/unparseable item for platform '$platform': ${e.message}")
            return false
        }

        if (downloadHistoryRepository.exists(platform, item.id)) {
            log.info("Skipping already downloaded item [platform=$platform, id=${item.id}]")
            return false
        }

        if (targetNewItems.containsKey(item.id)) {
            log.info("Skipping duplicate item in current scan [platform=$platform, id=${item.id}]")
            return false
        }

        targetNewItems[item.id] = item
        return true
    }
}