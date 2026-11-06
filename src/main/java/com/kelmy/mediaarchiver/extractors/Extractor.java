package com.kelmy.mediaarchiver.extractors;

import java.nio.file.Path;
import java.util.List;

import com.kelmy.mediaarchiver.schemas.BaseChannelConfig;

/**
 * Abstract Base Class that defines the contract
 * that any platform extractor must implement.
 */
public abstract class Extractor<TConfig extends BaseChannelConfig, TOptions> {
    protected final String name;

    protected Extractor(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    /**
     * Reads the platform's feed.
     *
     * @param targetFeed The URL of the channel or feed.
     * @param options    Specific search options (e.g., videos, shorts).
     * @return A list of the media items found.
     */
    public abstract List<String> getEndpointsToScan(String targetFeed, TOptions options);

    /**
     * Constructs the yt-dlp CLI arguments for the Light Scan.
     */
    public abstract List<String> getLightScanCommand(String endpoint);

    /**
     * Constructs a map of typed options for yt-dlp to execute the download.
     *
     * @param itemUrl     The URL of the video to be downloaded.
     * @param outputDir   The physical directory where the file will be saved.
     * @param archivePath The path to the archive.txt file.
     * @param cookiesPath An optional path to the cookies file.
     * @return A yt-dlp commands list.
     */
    public abstract List<String> getDownloadCommand(String itemUrl, Path outputDir, Path archivePath, Path cookiesPath);

    /**
     * Extracts specific options from the generic channel configuration.
     */
    public abstract TOptions extractOptions(TConfig channelConfig);
}
