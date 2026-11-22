package com.kelmy.mediaarchiver.archiving.infrastructure.extractor

import com.kelmy.mediaarchiver.archiving.domain.model.MediaItem
import com.kelmy.mediaarchiver.archiving.domain.port.Extractor
import tools.jackson.core.JacksonException
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.nio.file.Files
import java.nio.file.Path

/**
 * Base class of every extractor that is built on top of the `yt-dlp` command line tool.
 *
 * `yt-dlp` speaks the same language for every platform it supports, so everything that does not
 * depend on the platform lives here (**generalization**): the scan command, the download command and
 * the parsing of the JSON it prints. A concrete extractor (**specialization**) only says which URLs of a
 * channel must be scanned ([getEndpointsToScan]) and how to read a video id from a URL ([extractVideoId]).
 *
 * Adding support to another platform that `yt-dlp` knows is just a new subclass plus one line in
 * [InMemoryExtractorRegistry.createDefault].
 *
 * @param name platform identifier this extractor answers to (e.g. `youtube`). It is also the
 *             prefix `yt-dlp` writes in `archive.txt` (case-insensitive).
 */
internal abstract class YtDlpExtractor(override val name: String) : Extractor {
    private companion object {
        val objectMapper = jacksonObjectMapper()
    }

    /**
     * Builds a `yt-dlp` command that prints one JSON object per item without downloading anything
     * (`--flat-playlist --dump-json`).
     */
    final override fun getLightScanCommand(endpoint: String): List<String> {
        return listOf(
            "yt-dlp",
            "--flat-playlist",
            "--dump-json",
            "--no-warnings",
            "--quiet",
            endpoint
        )
    }

    /**
     * Builds the `yt-dlp` command that downloads [itemUrl] as `.mkv` (best video + best audio),
     * embedding thumbnail, metadata and all subtitles, and printing the final file path
     * (`--print after_move:filepath`). The cookies file is only used if it exists.
     */
    final override fun getDownloadCommand(
        itemUrl: String,
        outputDir: Path,
        archivePath: Path?,
        cookiesPath: Path?
    ): List<String> {
        val command = ArrayList<String>()
        command.add("yt-dlp")

        command.add("--format")
        command.add("bestvideo*+bestaudio/best")

        command.add("--merge-output-format")
        command.add("mkv")

        command.add("--newline")

        command.add("--write-info-json")
        command.add("--write-thumbnail")
        command.add("--embed-thumbnail")
        command.add("--embed-metadata")
        command.add("--embed-subs")
        command.add("--write-subs")

        command.add("--sub-langs")
        command.add("all")

        command.add("--paths")
        command.add("home:" + outputDir.toAbsolutePath().toString())

        if (archivePath != null) {
            command.add("--download-archive")
            command.add(archivePath.toAbsolutePath().toString())
        }

        if (cookiesPath != null && Files.exists(cookiesPath)) {
            command.add("--cookies")
            command.add(cookiesPath.toAbsolutePath().toString())
        }

        command.add("--print")
        command.add("after_move:filepath")

        command.add(itemUrl)

        return command
    }

    /**
     * Converts one line of `yt-dlp --dump-json` into a [MediaItem].
     *
     * The URL is `webpage_url` when present, otherwise `url`. A missing or blank `upload_date`
     * becomes `null`.
     *
     * @throws IllegalArgumentException if the line is not valid JSON, is not a JSON object, is a
     *                                  playlist entry, or lacks `id`, `title` or a URL.
     */
    final override fun parseItem(rawJson: String): MediaItem {
        val root: JsonNode = try {
            objectMapper.readTree(rawJson)
        } catch (e: JacksonException) {
            throw IllegalArgumentException("Invalid JSON returned by yt-dlp: ${e.message}", e)
        }

        if (!root.isObject) {
            throw IllegalArgumentException("Expected a JSON object, but got: ${root.nodeType}")
        }

        val entryType = root.path("_type").asString("")
        if (entryType.equals("playlist", ignoreCase = true)) {
            throw IllegalArgumentException("Input ignored because it is a playlist")
        }

        val id = root.path("id").asString("").trim()
        require(id.isNotEmpty()) { "'id' field missing or empty in $name JSON" }

        val title = root.path("title").asString("").trim()
        require(title.isNotEmpty()) { "'title' field missing or empty for id=$id" }

        val webpageUrl = root.path("webpage_url").asString("").trim()
        val directUrl = root.path("url").asString("").trim()
        val url = when {
            webpageUrl.isNotEmpty() -> webpageUrl
            directUrl.isNotEmpty() -> directUrl
            else -> throw IllegalArgumentException("No URL ('webpage_url' or 'url') found for id=$id")
        }

        val uploadDate = root.path("upload_date").takeIf { !it.isMissingNode && !it.isNull }
            ?.asString("")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        return MediaItem(
            id = id,
            title = title,
            url = url,
            extractor = this.name.lowercase(),
            uploadDate = uploadDate
        )
    }
}
