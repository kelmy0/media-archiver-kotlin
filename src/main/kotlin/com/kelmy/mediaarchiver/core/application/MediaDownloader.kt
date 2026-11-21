package com.kelmy.mediaarchiver.core.application

import com.kelmy.mediaarchiver.core.domain.port.Extractor
import com.kelmy.mediaarchiver.core.domain.model.MediaItem
import java.nio.file.Path

interface MediaDownloader {

    /**
     * downloads the content, saves it to the database
     * @return true if no errors occur
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