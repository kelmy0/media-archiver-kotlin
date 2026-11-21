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
     * Tells which of the given items were already downloaded, using as few queries as possible.
     * Prefer it over calling [exists] in a loop.
     *
     * @param platform platform identifier (e.g. `youtube`).
     * @param videoIds identifiers to look up. May be large; implementations split it as needed.
     * @return the subset of [videoIds] that is already in the history (empty if none, or if [videoIds] is empty).
     */
    fun findExistingIds(platform: String, videoIds: Collection<String>): Set<String>

    /**
     * Stores a download record.
     *
     * @return `true` if the record was stored.
     */
    fun save(model: DownloadHistory): Boolean
}
