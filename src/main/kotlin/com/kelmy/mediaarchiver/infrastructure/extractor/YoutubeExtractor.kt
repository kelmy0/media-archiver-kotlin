package com.kelmy.mediaarchiver.infrastructure.extractor

import com.kelmy.mediaarchiver.core.domain.model.Channel
import com.kelmy.mediaarchiver.core.domain.model.MediaItem
import com.kelmy.mediaarchiver.core.domain.port.Extractor
import java.nio.file.Path
import java.util.ArrayList

import org.slf4j.LoggerFactory
import tools.jackson.core.JacksonException

import tools.jackson.databind.DatabindException
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.nio.file.Files

/**
 * YouTube-specific options, read from [Channel.optionsJson].
 *
 * @property fetchVideos Scan the channel's `/videos` tab. Defaults to `true`.
 * @property fetchShorts Scan the channel's `/shorts` tab. Defaults to `false`.
 */
internal data class YoutubeFetchOptions(
    val fetchVideos: Boolean = true,
    val fetchShorts: Boolean = false
)

/**
 * [Extractor] for YouTube, built on top of the `yt-dlp` command line tool.
 *
 * @param name platform identifier this extractor answers to (normally `youtube`).
 */
class YoutubeExtractor(override val name: String) : Extractor {
    companion object {
        private val log = LoggerFactory.getLogger(YoutubeExtractor::class.java)
        private val objectMapper = jacksonObjectMapper()

        /** Compiled once; matches the slashes at the end of a URL. */
        private val TRAILING_SLASHES = Regex("/+$")
    }

    /**
     * Returns the URLs to scan:
     * - a playlist or single-video URL is scanned as it is;
     * - a channel URL is expanded into `/videos` and/or `/shorts`, depending on the channel options.
     *
     * @throws IllegalArgumentException if the channel options JSON has an invalid structure.
     * @throws RuntimeException if the options JSON cannot be processed.
     */
    override fun getEndpointsToScan(model: Channel): List<String> {
        val options = this.extractOptions(model)

        val endpoints = ArrayList<String>()
        var baseUrl = model.feedUrl.replace(TRAILING_SLASHES, "")

        if (baseUrl.contains("playlist?list=") || baseUrl.contains("watch?v=")) {
            endpoints.add(baseUrl)
        } else {
            baseUrl = baseUrl.replace("/videos", "").replace("/shorts", "")

            if (options.fetchVideos) {
                endpoints.add("$baseUrl/videos")
            }
            if (options.fetchShorts) {
                endpoints.add("$baseUrl/shorts")
            }
        }

        return endpoints
    }

    /**
     * Builds a `yt-dlp` command that prints one JSON object per item without downloading anything
     * (`--flat-playlist --dump-json`).
     */
    override fun getLightScanCommand(endpoint: String): List<String> {
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
    override fun getDownloadCommand(
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
     * Reads the YouTube options of [channelConfig]. A blank or empty (`{}`) JSON means "use the defaults".
     *
     * @throws IllegalArgumentException if the JSON does not match [YoutubeFetchOptions].
     * @throws RuntimeException for any other failure while reading the JSON.
     */
    private fun extractOptions(channelConfig: Channel): YoutubeFetchOptions {
        val jsonOptions = channelConfig.optionsJson

        if (jsonOptions.isBlank() || jsonOptions == "{}") {
            log.warn(
                "No JSON option found for channel '{}'. Using default values.",
                channelConfig.name
            )

            return YoutubeFetchOptions(fetchVideos = true, fetchShorts = false)
        }

        try {
            val options: YoutubeFetchOptions = objectMapper.readValue(jsonOptions, YoutubeFetchOptions::class.java)

            this.validateOptions(options)

            return options
        } catch (e: DatabindException) {
            log.error(
                "Structure/mapping error in the options JSON for the channel: {}. Details: {}",
                channelConfig.name, e
            )
            throw IllegalArgumentException("Invalid JSON configuration for the channel ${channelConfig.name}", e)
        } catch (e: Exception) {
            log.error(
                "Unexpected error while processing JSON options for the channel: {}", channelConfig.name,
                e
            )
            throw RuntimeException("Failed to read channel options", e)
        }
    }

    /** Only logs a warning when both tabs are disabled (nothing would be scanned); it does not reject the options. */
    private fun validateOptions(options: YoutubeFetchOptions) {
        if (!options.fetchVideos && !options.fetchShorts) {
            log.warn("Attention: Both 'fetchVideos' and 'fetchShorts' are disabled for this channel.")
        }
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
    override fun parseItem(rawJson: String): MediaItem {
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
        require(id.isNotEmpty()) { "'id' field missing or empty in YouTube JSON" }

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
