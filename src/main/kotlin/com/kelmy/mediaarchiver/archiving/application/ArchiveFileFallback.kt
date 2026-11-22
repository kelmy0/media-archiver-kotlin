package com.kelmy.mediaarchiver.archiving.application

import com.kelmy.mediaarchiver.archiving.domain.model.MediaItem
import com.kelmy.mediaarchiver.archiving.domain.port.ChannelFolderReader
import com.kelmy.mediaarchiver.archiving.domain.port.ImportedFoldersRepository
import com.kelmy.mediaarchiver.library.domain.model.Channel
import com.kelmy.mediaarchiver.library.domain.model.Video
import com.kelmy.mediaarchiver.library.domain.port.VideoRepository
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.time.Clock
import java.time.Instant

/**
 * Second source of "what was already downloaded", used when the database does not know an item.
 *
 * Before this app existed, a channel folder may already hold videos downloaded with plain `yt-dlp`,
 * which keeps an `archive.txt` there. For the items that the database does not know yet, this class
 * reads that file **once per folder**, registers the matching items in the database (so from then on
 * the database alone is enough) and removes them from the list of items to download.
 *
 * "Once per folder" is controlled by [ImportedFoldersRepository]: after the first look the folder is
 * marked and the file is never read again.
 *
 * Only items that appear in the current scan are registered, because they are the only ones that
 * could be downloaded again; anything else in the file can be registered by hand later.
 *
 * @property folders  reads `archive.txt` and looks for the video files.
 * @property imported remembers which folders were already imported.
 * @property videos   where the discovered videos are registered.
 * @property clock    time source for `downloadedAt` (injectable for tests).
 */
internal class ArchiveFileFallback(
    private val folders: ChannelFolderReader,
    private val imported: ImportedFoldersRepository,
    private val videos: VideoRepository,
    private val clock: Clock = Clock.systemUTC()
) {
    private companion object {
        val log = LoggerFactory.getLogger(ArchiveFileFallback::class.java)
    }

    /**
     * Registers the [items] that `archive.txt` says were already downloaded and removes them from [items].
     * Does nothing when [items] is empty or when the folder was already imported.
     *
     * @param channel    channel that owns the folder (the registered videos are attached to it).
     * @param channelDir folder of the channel.
     * @param items      items that the database did not know; modified in place.
     * @return how many items were registered.
     */
    fun registerKnownItems(channel: Channel, channelDir: Path, items: MutableMap<String, MediaItem>): Int {
        if (items.isEmpty() || imported.isImported(channel.folderName)) return 0

        val archivedIds = folders.readArchivedIds(channelDir, channel.platform)
        var registered = 0

        for (id in items.keys.filter { it in archivedIds }) {
            val item = items.getValue(id)
            val filePath = folders.findMediaFile(channelDir, id) ?: ""

            val video = Video(
                platform = channel.platform,
                videoId = id,
                channelId = channel.id,
                title = item.title,
                filePath = filePath,
                sourceUrl = item.url,
                downloadedAt = Instant.now(clock)
            )

            if (videos.insert(video)) {
                items.remove(id)
                registered++
                log.info("Registered from archive.txt [platform={}, id={}]", channel.platform, id)
            }
        }

        imported.markImported(channel.folderName)
        log.info("archive.txt of folder '{}' imported: {} item(s) registered.", channel.folderName, registered)
        return registered
    }
}
