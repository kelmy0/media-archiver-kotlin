package com.kelmy.mediaarchiver.core

import com.kelmy.mediaarchiver.extractors.Extractor
import com.kelmy.mediaarchiver.model.MediaItemModel
import java.nio.file.Path

interface MediaDownloaderProvider {

    /**
     * downloads the content, saves it to the database
     * @return true if no errors occur
     */
    fun downloadAndRecord(
        item: MediaItemModel,
        platform: String,
        extractor: Extractor,
        outputDir: Path,
        archivePath: Path?,
        cookiesPath: Path? = null
    ): Boolean
}