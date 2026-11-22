package com.kelmy.mediaarchiver.archiving.application

import com.kelmy.mediaarchiver.archiving.domain.model.MediaItem
import com.kelmy.mediaarchiver.archiving.domain.port.Extractor

/**
 * Turns the raw output of a scan into the list of items that really need to be downloaded.
 *
 * It works in two steps: [collectFromLine] is called for every output line while the scan runs,
 * and [removeAlreadyDownloaded] is called once, after the scan, to discard what is already in the
 * registered videos.
 */
interface MediaCollector {
    /**
     * Processes one raw JSON line of the scan output:
     * 1. parses it with the [extractor];
     * 2. discards it if it was already collected in the current scan;
     * 3. otherwise adds it to [targetItems].
     *
     * It does **not** look at the registered videos; call [removeAlreadyDownloaded] after the scan.
     *
     * @param rawLine     one line produced by the scan command.
     * @param platform    platform of the channel being scanned (used in log messages).
     * @param extractor   extractor used to parse the line.
     * @param targetItems accumulator of scanned items, keyed by item id (insertion order is preserved
     *                    when a [LinkedHashMap] is used).
     * @return `true` if the item was added, `false` if it was ignored (blank, invalid or duplicate in this scan).
     */
    fun collectFromLine(
        rawLine: String,
        platform: String,
        extractor: Extractor,
        targetItems: MutableMap<String, MediaItem>
    ): Boolean

    /**
     * Removes from [items] every item that is already in the registered videos, using a single batched
     * lookup instead of one query per item. The relative order of the remaining items is kept.
     *
     * @param platform platform of the channel that was scanned.
     * @param items    items collected by [collectFromLine]; modified in place.
     * @return how many items were removed.
     * @throws RuntimeException if the history cannot be queried (the map is left untouched).
     */
    fun removeAlreadyDownloaded(platform: String, items: MutableMap<String, MediaItem>): Int
}
