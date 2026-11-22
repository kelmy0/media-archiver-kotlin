package com.kelmy.mediaarchiver.library.domain.port

import com.kelmy.mediaarchiver.library.domain.model.Channel
import com.kelmy.mediaarchiver.library.domain.model.NewChannel
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

    /** Returns the channel with this [id], or `null` if there is none. Its video list is not loaded. */
    fun findById(id: Int): Channel?

    /** Returns the channel registered with exactly this [feedUrl], or `null`. Its video list is not loaded. */
    fun findByFeedUrl(feedUrl: String): Channel?

    /** Returns `true` if a channel with exactly this [feedUrl] is already registered. */
    fun existsByFeedUrl(feedUrl: String): Boolean

    /**
     * Sets `lastChecked` to [timestamp] for all given [channels], atomically.
     * Does nothing when [channels] is empty.
     */
    fun updateLastChecked(channels: List<Channel>, timestamp: Instant)

    /**
     * Removes a channel **and the records of its videos** (the files on disk are never touched).
     *
     * @return `true` if a channel was deleted.
     */
    fun deleteChannel(id: Int): Boolean
}
