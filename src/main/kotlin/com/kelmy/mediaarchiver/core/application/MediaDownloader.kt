package com.kelmy.mediaarchiver.core.application

import com.kelmy.mediaarchiver.core.domain.port.Extractor
import com.kelmy.mediaarchiver.core.domain.model.MediaItem
import java.nio.file.Path

/**
 * Downloads one item and records it in the download history.
 */
interface MediaDownloader {

    /**
     * Downloads [item] and, on success, saves it to the download history.
     *
     * @param item        the item to download.
     * @param platform    platform of the channel the item belongs to.
     * @param extractor   extractor that builds the download command.
     * @param outputDir   directory where the files are saved.
     * @param archivePath path of the `archive.txt` file, or `null` to not use one.
     * @param cookiesPath optional path of a cookies file.
     * @return `true` if the item was downloaded **and** recorded; `false` on any failure
     *         (the failure is logged, never thrown).
     */
    fun downloadAndRecord(
        item: MediaItem,
        platform: String,
        extractor: Extractor,
        outputDir: Path,
        archivePath: Path?,
        cookiesPath: Path? = null
    ): Boolean
}
