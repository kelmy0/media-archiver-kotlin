package com.kelmy.mediaarchiver.library.application

import com.kelmy.mediaarchiver.library.domain.model.Channel
import com.kelmy.mediaarchiver.library.domain.model.NewChannel
import com.kelmy.mediaarchiver.library.domain.port.ChannelsRepository
import com.kelmy.mediaarchiver.library.domain.port.VideoRepository

/**
 * Use cases of the channel CRUD. The presentation layer talks to this class instead of talking
 * to the repositories, so the rules live in one place whatever the interface (textual or graphical).
 *
 * @property channels repository of channels.
 * @property videos   repository of videos, used to fill the channel's video list.
 */
class ChannelService(
    private val channels: ChannelsRepository,
    private val videos: VideoRepository
) {
    /** **Create**: registers a new channel. [NewChannel] has already validated its own fields. */
    fun create(channel: NewChannel): Channel = channels.insert(channel)

    /** **Read**: every channel, active or not. Their video lists are not loaded (see [listWithVideos]). */
    fun list(): List<Channel> = channels.getChannels()

    /** **Read**: one channel by id, without its videos, or `null` if it does not exist. */
    fun find(id: Int): Channel? = channels.findById(id)

    /** **Read**: one channel by feed URL, without its videos, or `null` if it does not exist. */
    fun findByFeedUrl(feedUrl: String): Channel? = channels.findByFeedUrl(feedUrl)

    /**
     * **Read**: one channel with its [Channel.videos] list filled in (the aggregate is complete).
     *
     * @return the channel, or `null` if it does not exist.
     */
    fun findWithVideos(id: Int): Channel? = channels.findById(id)?.also(::loadVideos)

    /** **Read**: every channel with its video list filled in. Used by the backup. */
    fun listWithVideos(): List<Channel> = channels.getChannels().onEach(::loadVideos)

    /** **Update**: saves the new data of an existing channel. @return `true` if the channel existed. */
    fun update(channel: Channel): Boolean = channels.updateChannel(channel)

    /**
     * **Delete**: removes the channel **and the records of its videos**. Files on disk are kept.
     *
     * @return `true` if the channel existed.
     */
    fun delete(id: Int): Boolean = channels.deleteChannel(id)

    /** Fills the video list of [channel] from the repository. */
    private fun loadVideos(channel: Channel) {
        videos.findByChannel(channel.id).forEach(channel::addVideo)
    }
}
