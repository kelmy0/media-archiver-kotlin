package com.kelmy.mediaarchiver.core

import com.kelmy.mediaarchiver.extractors.Extractor
import com.kelmy.mediaarchiver.model.MediaItemModel
import com.kelmy.mediaarchiver.repositories.DownloadHistoryRepositoryProvider
import org.slf4j.LoggerFactory

class MediaCollector(
    private val downloadHistoryRepository: DownloadHistoryRepositoryProvider
) : MediaCollectorProvider {
    companion object {
        private val log = LoggerFactory.getLogger(MediaCollector::class.java)
    }

    override fun collectFromLine(
        rawLine: String,
        platform: String,
        extractor: Extractor,
        targetNewItems: MutableMap<String, MediaItemModel>
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