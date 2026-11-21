package com.kelmy.mediaarchiver.core.domain.port

import com.kelmy.mediaarchiver.core.domain.model.DownloadHistory

/**
 * Port for the history of downloaded items. It is the source of truth used to
 * avoid downloading the same media twice.
 */
interface DownloadHistoryRepository {
    /**
     * Checks whether an item was already downloaded.
     *
     * @param platform platform identifier (e.g. `youtube`).
     * @param videoId  identifier of the item on that platform.
     * @return `true` if the history already contains the item.
     */
    fun exists(platform: String, videoId: String): Boolean

    /**
     * Stores a download record.
     *
     * @return `true` if the record was stored.
     */
    fun save(model: DownloadHistory): Boolean
}