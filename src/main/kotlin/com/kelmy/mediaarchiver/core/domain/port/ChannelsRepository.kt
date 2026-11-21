package com.kelmy.mediaarchiver.core.domain.port

import com.kelmy.mediaarchiver.core.domain.model.Channel
import com.kelmy.mediaarchiver.core.domain.model.NewChannel
import java.time.Instant

/**
 * Port for persisting and querying monitored channels.
 */
interface ChannelsRepository {
    /**
     * Persists a new channel.
     *
     * @return the stored channel, including its generated `id` and `lastChecked = Instant.EPOCH`.
     */
    fun insert(channel: NewChannel): Channel

    /** Returns every registered channel, active or not. */
    fun getChannels(): List<Channel>

    /** Returns only the channels flagged as active. */
    fun getActiveChannels(): List<Channel>

    /**
     * Updates an existing channel.
     *
     * @return `true` if a channel was updated.
     */
    fun updateChannel(channel: Channel): Boolean

    /** Returns `true` if a channel with exactly this [feedUrl] is already registered. */
    fun existsByFeedUrl(feedUrl: String): Boolean

    /**
     * Sets `lastChecked` to [timestamp] for all given [channels], atomically.
     * Does nothing when [channels] is empty.
     */
    fun updateLastChecked(channels: List<Channel>, timestamp: Instant)

    /**
     * Removes a channel.
     *
     * @return `true` if a channel was deleted.
     */
    fun deleteChannel(id: Int): Boolean
}
