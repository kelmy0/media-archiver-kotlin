package com.kelmy.mediaarchiver.core.application

import com.kelmy.mediaarchiver.core.domain.port.Extractor
import com.kelmy.mediaarchiver.core.domain.model.MediaItem

/**
 * Turns the raw output of a scan into the list of items that really need to be downloaded.
 */
interface MediaCollector {
    /**
     * Processes one raw JSON line of the scan output:
     * 1. parses it with the [extractor];
     * 2. discards it if it was already collected in the current scan;
     * 3. discards it if it is already in the download history;
     * 4. otherwise adds it to [targetNewItems].
     *
     * @param rawLine        one line produced by the scan command.
     * @param platform       platform of the channel being scanned.
     * @param extractor      extractor used to parse the line.
     * @param targetNewItems accumulator of new items, keyed by item id (insertion order is preserved
     *                       when a [LinkedHashMap] is used).
     * @return `true` if the item was added as new, `false` if it was ignored (blank, invalid or duplicate).
     */
    fun collectFromLine(
        rawLine: String,
        platform: String,
        extractor: Extractor,
        targetNewItems: MutableMap<String, MediaItem>
    ): Boolean
}
