package com.kelmy.mediaarchiver.library.application

import com.kelmy.mediaarchiver.library.domain.model.Video
import com.kelmy.mediaarchiver.library.domain.port.ChannelsRepository
import com.kelmy.mediaarchiver.library.domain.port.VideoIdResolver
import com.kelmy.mediaarchiver.library.domain.port.VideoRepository
import java.time.Clock
import java.time.Instant

/**
 * Use cases of the video CRUD.
 *
 * Videos are normally created by the archiving routine (download). The **create** operation exposed
 * here covers the other case: a video that already exists on disk (for instance in another folder)
 * and must be associated to a channel by hand ([associate]).
 *
 * Every failure caused by invalid input is reported with [IllegalArgumentException], so any
 * interface can show the message to the user.
 *
 * @property videos   repository of videos.
 * @property channels repository of channels, used to validate the target channel.
 * @property resolver discovers the video id from its URL.
 * @property clock    time source for `downloadedAt` (injectable for tests).
 */
class VideoService(
    private val videos: VideoRepository,
    private val channels: ChannelsRepository,
    private val resolver: VideoIdResolver,
    private val clock: Clock = Clock.systemUTC()
) {
    /**
     * **Create**: associates an existing video to a channel.
     *
     * The platform comes from the channel and the id is discovered from [url]. Registering the
     * video also tells the archiving routine not to download it again.
     *
     * @param channelId channel that will own the video.
     * @param title     video title.
     * @param url       URL of the video on its platform (cannot be changed later).
     * @param filePath  where the video file is on disk.
     * @return the stored video.
     * @throws IllegalArgumentException if the channel does not exist, a field is blank, the URL is not a
     *                                  video URL of the channel's platform, or the video is already registered.
     */
    fun associate(channelId: Int, title: String, url: String, filePath: String): Video {
        val channel = channels.findById(channelId)
            ?: throw IllegalArgumentException("Channel #$channelId does not exist")
        require(filePath.isNotBlank()) { "filePath cant be blank" }

        val videoId = resolver.resolve(channel.platform, url.trim())
            ?: throw IllegalArgumentException("Could not find a '${channel.platform}' video id in the URL: $url")

        require(!videos.exists(channel.platform, videoId)) {
            "Video '$videoId' is already registered"
        }

        val video = Video(
            platform = channel.platform,
            videoId = videoId,
            channelId = channel.id,
            title = title.trim(),
            filePath = filePath.trim(),
            sourceUrl = url.trim(),
            downloadedAt = Instant.now(clock)
        )
        channel.ensureCanHold(video)
        videos.insert(video)
        return video
    }

    /** **Read**: the videos of a channel, newest first. */
    fun listByChannel(channelId: Int): List<Video> = videos.findByChannel(channelId)

    /** **Read**: one video, or `null` if it does not exist. */
    fun find(platform: String, videoId: String): Video? = videos.find(platform, videoId)

    /**
     * **Update**: changes the title and/or the channel of a video.
     * The URL and the id are the video's identity, so they cannot be edited.
     *
     * @param newChannelId channel the video should belong to afterwards (may be the current one).
     * @return the updated video.
     * @throws IllegalArgumentException if the video or the target channel does not exist, the title is
     *                                  blank, or the target channel belongs to another platform.
     */
    fun edit(platform: String, videoId: String, newTitle: String, newChannelId: Int): Video {
        val current = videos.find(platform, videoId)
            ?: throw IllegalArgumentException("Video '$videoId' does not exist")
        val target = channels.findById(newChannelId)
            ?: throw IllegalArgumentException("Channel #$newChannelId does not exist")

        val updated = current.copy(title = newTitle.trim(), channelId = target.id)
        target.ensureCanHold(updated)

        check(videos.update(updated)) { "Video '$videoId' could not be updated" }
        return updated
    }

    /**
     * **Delete**: removes the record of the video. The file on disk is kept.
     *
     * @return `true` if the video existed.
     */
    fun delete(platform: String, videoId: String): Boolean = videos.delete(platform, videoId)
}
