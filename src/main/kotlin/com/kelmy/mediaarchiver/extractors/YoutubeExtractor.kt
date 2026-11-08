package com.kelmy.mediaarchiver.extractors

import java.nio.file.Path
import java.util.ArrayList

import org.slf4j.LoggerFactory

import com.kelmy.mediaarchiver.schemas.BaseChannelConfig
import com.kelmy.mediaarchiver.schemas.YoutubeFetchOptions

import tools.jackson.databind.DatabindException
import tools.jackson.databind.ObjectMapper

class YoutubeExtractor(name: String) : Extractor<YoutubeFetchOptions>(name = name) {
    companion object {
        private val log = LoggerFactory.getLogger(YoutubeExtractor::class.java)
        private val objectMapper = ObjectMapper()
    }


    override fun getEndpointsToScan(targetFeed: String, options: YoutubeFetchOptions): List<String> {
        val endpoints = ArrayList<String>()
        var baseUrl = targetFeed.replace(Regex("/+$"), "")

        if (baseUrl.contains("playlist?list=") || baseUrl.contains("watch?v=")) {
            endpoints.add(baseUrl)
        } else {
            baseUrl = baseUrl.replace("/videos", "").replace("/shorts", "")

            if (options.fetchVideos) {
                endpoints.add("${baseUrl}/videos")
            }
            if (options.fetchShorts) {
                endpoints.add("${baseUrl}/shorts")
            }
        }

        return endpoints
    }

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

        command.add("--write-info-json")
        command.add("--write-thumbnail")
        command.add("--embed-thumbnail")
        command.add("--embed-metadata")
        command.add("--embed-subs")
        command.add("--write-subtitles")

        command.add("--subtitles-langs")
        command.add("all")

        command.add("--paths")
        command.add("home:" + outputDir.toAbsolutePath().toString())


        if (archivePath != null) {
            command.add("--download-archive")
            command.add(archivePath.toAbsolutePath().toString())
        }

        if (cookiesPath != null && java.nio.file.Files.exists(cookiesPath)) {
            command.add("--cookies")
            command.add(cookiesPath.toAbsolutePath().toString())
        }

        command.add(itemUrl)

        return command
    }

    override fun extractOptions(channelConfig: BaseChannelConfig): YoutubeFetchOptions {
        val jsonOptions = channelConfig.optionsJson

        if (jsonOptions.isBlank()) {
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
                "Eunexpected error while processing JSON options for the channel: {}", channelConfig.name,
                e
            )
            throw RuntimeException("Failed to read channel options", e)
        }
    }

    private fun validateOptions(options: YoutubeFetchOptions) {
        if (!options.fetchVideos && !options.fetchShorts) {
            log.warn("Attention: Both 'fetchVideos' and 'fetchShorts' are disabled for this channel.")
        }
    }
}
