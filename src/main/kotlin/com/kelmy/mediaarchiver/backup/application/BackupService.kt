package com.kelmy.mediaarchiver.backup.application

import com.kelmy.mediaarchiver.backup.domain.port.BackupStore
import com.kelmy.mediaarchiver.library.application.ChannelService
import com.kelmy.mediaarchiver.library.domain.model.NewChannel
import com.kelmy.mediaarchiver.library.domain.port.VideoRepository
import java.nio.file.Path

/**
 * What an import did.
 *
 * @property channelsAdded   channels that did not exist and were created.
 * @property channelsSkipped channels that already existed (same feed URL); they are kept as they are.
 * @property videosAdded     videos that were registered.
 * @property videosSkipped   videos that were already registered (same platform and id).
 */
data class ImportReport(
    val channelsAdded: Int,
    val channelsSkipped: Int,
    val videosAdded: Int,
    val videosSkipped: Int
)

/**
 * Use cases "save all data to a file" and "load data from a file", for every business class
 * (channels and their videos).
 *
 * @property channels service used to read and create channels.
 * @property videos   repository used to register the imported videos.
 * @property store    reads and writes the file.
 */
class BackupService(
    private val channels: ChannelService,
    private val videos: VideoRepository,
    private val store: BackupStore
) {
    /**
     * Saves every channel with all its videos to [target].
     *
     * @return how many channels were saved.
     * @throws java.io.IOException if the file cannot be written.
     */
    fun saveTo(target: Path): Int {
        val all = channels.listWithVideos()
        store.write(target, all)
        return all.size
    }

    /**
     * Loads [source] and merges it into the database. It never overwrites anything: a channel is matched by
     * its feed URL and a video by (platform, id); whatever already exists is skipped, so importing the
     * same file twice is harmless. Videos of an existing channel are added to that channel.
     *
     * @throws java.io.IOException if the file cannot be read.
     * @throws IllegalArgumentException if the file does not have the expected format.
     */
    fun loadFrom(source: Path): ImportReport {
        var channelsAdded = 0
        var channelsSkipped = 0
        var videosAdded = 0
        var videosSkipped = 0

        for (fromFile in store.read(source)) {
            val existing = channels.findByFeedUrl(fromFile.feedUrl)
            val target = existing ?: channels.create(
                NewChannel(
                    name = fromFile.name,
                    platform = fromFile.platform,
                    feedUrl = fromFile.feedUrl,
                    folderName = fromFile.folderName,
                    active = fromFile.active,
                    optionsJson = fromFile.optionsJson
                )
            )
            if (existing == null) channelsAdded++ else channelsSkipped++

            for (video in fromFile.videos) {
                if (videos.exists(video.platform, video.videoId)) {
                    videosSkipped++
                } else {
                    videos.insert(video.copy(channelId = target.id))
                    videosAdded++
                }
            }
        }
        return ImportReport(channelsAdded, channelsSkipped, videosAdded, videosSkipped)
    }
}
