package com.kelmy.mediaarchiver.archiving.application

import com.kelmy.mediaarchiver.archiving.domain.port.Extractor
import com.kelmy.mediaarchiver.archiving.domain.model.MediaItem
import java.nio.file.Path

/**
 * Downloads one item and registers it as a [com.kelmy.mediaarchiver.library.domain.model.Video] of its channel.
 */
interface MediaDownloader {

    /**
     * Downloads [item] and, on success, saves it as a video of the channel [channelId].
     *
     * @param item        the item to download.
     * @param platform    platform of the channel the item belongs to.
     * @param channelId   id of the channel that will own the saved video.
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
        channelId: Int,
        extractor: Extractor,
        outputDir: Path,
        archivePath: Path?,
        cookiesPath: Path? = null
    ): Boolean
}
