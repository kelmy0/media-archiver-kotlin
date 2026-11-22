package com.kelmy.mediaarchiver.archiving.domain.port

import com.kelmy.mediaarchiver.library.domain.model.Channel
import com.kelmy.mediaarchiver.archiving.domain.model.MediaItem
import java.nio.file.Path

/**
 * Contract that every platform extractor (YouTube, Bilibili...) must fulfil.
 *
 * An extractor knows **how** to talk to one platform: which URLs to scan, which command
 * line to run and how to interpret the output. It does not run anything itself; running
 * the commands is the job of a [CommandExecutor].
 */
interface Extractor {
    /** Platform identifier (e.g. `youtube`). It is matched against [Channel.platform]. */
    val name: String

    /**
     * Lists the URLs that must be scanned for a channel, according to its options.
     *
     * @param model the channel being processed.
     * @return one URL per feed to be scanned (e.g. `/videos` and `/shorts`).
     */
    fun getEndpointsToScan(model: Channel): List<String>

    /**
     * Builds the command line of the lightweight scan, which only lists items
     * (one JSON object per output line) without downloading anything.
     *
     * @param endpoint a URL returned by [getEndpointsToScan].
     */
    fun getLightScanCommand(endpoint: String): List<String>

    /**
     * Builds the command line that downloads one item.
     *
     * @param itemUrl     URL of the item to be downloaded.
     * @param outputDir   directory where the files will be saved.
     * @param archivePath path of the `archive.txt` file, or `null` to not use one.
     * @param cookiesPath optional path of a cookies file.
     * @return the command and its arguments.
     */
    fun getDownloadCommand(
        itemUrl: String,
        outputDir: Path,
        archivePath: Path?,
        cookiesPath: Path?
    ): List<String>

    /**
     * Converts one line of the light scan output into a [MediaItem].
     *
     * @param rawJson a single JSON line produced by the scan command.
     * @throws IllegalArgumentException if the line is not a valid item (invalid JSON, a playlist,
     *                                  or missing id/title/URL).
     */
    fun parseItem(rawJson: String): MediaItem

    /**
     * Extracts the platform id of a video from the URL of that single video.
     * The library uses it to register a video by hand, and the id must be the same one that
     * [parseItem] would produce for that video, so both paths share the same identity.
     *
     * @param url URL of a video (e.g. `https://www.youtube.com/watch?v=...`).
     * @return the id, or `null` if [url] is not a video URL of this platform.
     */
    fun extractVideoId(url: String): String?
}
