package com.kelmy.mediaarchiver.archiving.domain.port

import java.nio.file.Path

/**
 * Port for reading what is already on disk in a channel's folder.
 *
 * It is the source of the "fallback" lookup: when a video is not in the database, the archiving
 * routine asks the folder whether `yt-dlp` had already downloaded it (see `ArchiveFileFallback`).
 */
interface ChannelFolderReader {
    /**
     * Reads the ids listed in the `archive.txt` file that `yt-dlp` keeps in [channelDir]
     * (`--download-archive`). Every line has the form `<extractor> <id>`.
     *
     * @param channelDir folder of the channel.
     * @param platform   only lines written by this extractor are returned (case-insensitive).
     * @return the ids, or an empty set if the folder or the file does not exist.
     */
    fun readArchivedIds(channelDir: Path, platform: String): Set<String>

    /**
     * Looks for the `.mkv` file of [videoId] in [channelDir] (yt-dlp names it `Title [id].mkv`).
     *
     * @return the absolute path of the file, or `null` if there is none.
     */
    fun findMediaFile(channelDir: Path, videoId: String): String?
}
