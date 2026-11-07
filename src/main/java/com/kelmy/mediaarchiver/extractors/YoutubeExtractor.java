package com.kelmy.mediaarchiver.extractors;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kelmy.mediaarchiver.schemas.BaseChannelConfig;
import com.kelmy.mediaarchiver.schemas.YoutubeFetchOptions;

import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ObjectMapper;

public class YoutubeExtractor extends Extractor<YoutubeFetchOptions> {
    private static final Logger log = LoggerFactory.getLogger(YoutubeExtractor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public YoutubeExtractor() {
        super("youtube");
    }

    @Override
    public List<String> getEndpointsToScan(String targetFeed, YoutubeFetchOptions options) {
        List<String> endpoints = new ArrayList<>();
        String baseUrl = targetFeed.replaceAll("/+$", "");

        if (baseUrl.contains("playlist?list=") || baseUrl.contains("watch?v=")) {
            endpoints.add(baseUrl);
        } else {
            baseUrl = baseUrl.replace("/videos", "").replace("/shorts", "");

            if (options.fetchVideos()) {
                endpoints.add(baseUrl + "/videos");
            }
            if (options.fetchShorts()) {
                endpoints.add(baseUrl + "/shorts");
            }
        }

        return endpoints;
    }

    public List<String> getLightScanCommand(String endpoint) {
        return List.of(
                "yt-dlp",
                "--flat-playlist",
                "--dump-json",
                "--no-warnings",
                "--quiet",
                endpoint);
    }

    @Override
    public List<String> getDownloadCommand(String itemUrl, Path outputDir, Path archivePath, Path cookiesPath) {
        List<String> command = new ArrayList<>();
        command.add("yt-dlp");

        command.add("--format");
        command.add("bestvideo*+bestaudio/best");

        command.add("--merge-output-format");
        command.add("mkv");

        command.add("--write-info-json");
        command.add("--write-thumbnail");
        command.add("--embed-thumbnail");
        command.add("--embed-metadata");
        command.add("--embed-subs");
        command.add("--write-subtitles");

        command.add("--subtitles-langs");
        command.add("all");

        if (outputDir != null) {
            command.add("--paths");
            command.add("home:" + outputDir.toAbsolutePath().toString());
        }

        if (archivePath != null) {
            command.add("--download-archive");
            command.add(archivePath.toAbsolutePath().toString());
        }

        if (cookiesPath != null && java.nio.file.Files.exists(cookiesPath)) {
            command.add("--cookies");
            command.add(cookiesPath.toAbsolutePath().toString());
        }

        command.add(itemUrl);

        return command;
    }

    @Override
    public YoutubeFetchOptions extractOptions(BaseChannelConfig channelConfig) {
        String jsonOptions = channelConfig != null ? channelConfig.getName() : null;

        if (jsonOptions == null || jsonOptions.isBlank()) {
            log.warn("No JSON option found for channel '{}'. Using default values.",
                    channelConfig != null ? channelConfig.getName() : "unknown");
            return new YoutubeFetchOptions(true, false);
        }

        try {
            YoutubeFetchOptions options = objectMapper.readValue(jsonOptions, YoutubeFetchOptions.class);

            this.validateOptions(options);

            return options;
        } catch (DatabindException e) {
            log.error("Structure/mapping error in the options JSON for the channel: {}. Details: {}",
                    channelConfig.getName(), e.getMessage());
            throw new IllegalArgumentException("Invalid JSON configuration for the channel " + channelConfig.getName(),
                    e);
        } catch (Exception e) {
            log.error("Eunexpected error while processing JSON options for the channel: {}", channelConfig.getName(),
                    e);
            throw new RuntimeException("Failed to read channel options", e);
        }
    }

    private void validateOptions(YoutubeFetchOptions options) {
        if (!options.fetchVideos() && !options.fetchShorts()) {
            log.warn("Attention: Both 'fetchVideos' and 'fetchShorts' are disabled for this channel.");
        }
    }
}
