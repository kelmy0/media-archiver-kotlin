package com.kelmy.mediaarchiver.core.application

import com.kelmy.mediaarchiver.core.domain.port.Extractor
import com.kelmy.mediaarchiver.core.domain.model.MediaItem

interface MediaCollector {
    /**
     * Processes a raw JSON line from the stream:
     * 1. Parses it using the extractor
     * 2. Checks the database to see if it has already been downloaded
     * 3. If it is new, adds it to the map
     *
     * @return true if the item was added as new, false if ignored/duplicate/invalid
     */
    fun collectFromLine(
        rawLine: String,
        platform: String,
        extractor: Extractor,
        targetNewItems: MutableMap<String, MediaItem>
    ): Boolean
}