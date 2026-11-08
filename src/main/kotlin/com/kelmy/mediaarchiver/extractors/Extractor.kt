package com.kelmy.mediaarchiver.extractors

import java.nio.file.Path

import com.kelmy.mediaarchiver.schemas.BaseChannelConfig

/**
 * Abstract Base Class that defines the contract
 * that any platform extractor must implement.
 */
abstract class Extractor<TOptions>(
    val name: String
) {
    /**
     * Reads the platform's feed.
     *
     * @param targetFeed The URL of the channel or feed.
     * @param options    Specific search options (e.g., videos, shorts).
     * @return A list of the media items found.
     */
    abstract fun getEndpointsToScan(targetFeed: String, options: TOptions): List<String>

    /**
     * Constructs the yt-dlp CLI arguments for the Light Scan.
     */
    abstract fun getLightScanCommand(endpoint: String): List<String>

    /**
     * Constructs a map of typed options for yt-dlp to execute the download.
     *
     * @param itemUrl     The URL of the video to be downloaded.
     * @param outputDir   The physical directory where the file will be saved.
     * @param archivePath The path to the archive.txt file.
     * @param cookiesPath An optional path to the cookies file.
     * @return A yt-dlp commands list.
     */
    abstract fun getDownloadCommand(
        itemUrl: String,
        outputDir: Path,
        archivePath: Path?,
        cookiesPath: Path?
    ): List<String>

    /**
     * Extracts specific options from the generic channel configuration.
     */
    abstract fun extractOptions(channelConfig: BaseChannelConfig): TOptions
}
