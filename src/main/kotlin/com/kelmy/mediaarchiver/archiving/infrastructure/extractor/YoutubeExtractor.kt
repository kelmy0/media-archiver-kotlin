package com.kelmy.mediaarchiver.archiving.infrastructure.extractor

import com.kelmy.mediaarchiver.library.domain.model.Channel
import org.slf4j.LoggerFactory
import tools.jackson.databind.DatabindException
import tools.jackson.module.kotlin.jacksonObjectMapper

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
 * [com.kelmy.mediaarchiver.archiving.domain.port.Extractor] for YouTube.
 *
 * The `yt-dlp` commands and the JSON parsing are inherited from [YtDlpExtractor]; this class adds
 * what is specific to YouTube: the `/videos` and `/shorts` tabs of a channel, the per-channel options
 * and the shape of video URLs.
 *
 * @param name platform identifier this extractor answers to (normally `youtube`).
 */
internal class YoutubeExtractor(name: String) : YtDlpExtractor(name) {
    private companion object {
        private val log = LoggerFactory.getLogger(YoutubeExtractor::class.java)
        val objectMapper = jacksonObjectMapper()

        /** Compiled once; matches the slashes at the end of a URL. */
        val TRAILING_SLASHES = Regex("/+$")

        /** The 11-character video id in `watch?v=`, `youtu.be/`, `/shorts/`, `/live/` and `/embed/` URLs. */
        val VIDEO_ID = Regex("""(?:youtu\.be/|[?&]v=|/shorts/|/live/|/embed/)([\w-]{11})""")
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

    override fun extractVideoId(url: String): String? = VIDEO_ID.find(url)?.groupValues?.get(1)

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
}
