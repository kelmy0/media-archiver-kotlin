package com.kelmy.mediaarchiver.extractors;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.kelmy.mediaarchiver.schemas.YoutubeChannelConfig;
import com.kelmy.mediaarchiver.schemas.YoutubeFetchOptions;

public class YoutubeExtractor extends Extractor<YoutubeChannelConfig, YoutubeFetchOptions> {
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
    public YoutubeFetchOptions extractOptions(YoutubeChannelConfig channelConfig) {
        boolean fetchVideos = channelConfig != null ? channelConfig.isFetchVideos() : true;
        boolean fetchShorts = channelConfig != null ? channelConfig.isFetchShorts() : false;

        return new YoutubeFetchOptions(fetchVideos, fetchShorts);
    }
}
