package com.kelmy.mediaarchiver.library.domain.model

import java.time.Duration
import java.time.Instant
import java.util.Collections

/**
 * A monitored channel (or playlist) that is already persisted.
 *
 * Instances are validated on construction through [ChannelValidator].
 *
 * Relationship with [Video]: **aggregation**. The channel is the aggregate root and keeps the list
 * of its [videos]. The list lives in the class body (not in the constructor), so it is **not** part of
 * `equals`/`hashCode`/`toString`/`copy`: two channels are equal when their own data is equal, and the
 * list is only filled when someone needs it (see [addVideo]); repositories do not load it by default,
 * because a channel can own thousands of videos.
 *
 * @property id          Database identifier.
 * @property name        Display name of the channel.
 * @property platform    Source platform identifier (e.g. `youtube`). It must match the
 *                       name of a registered extractor.
 * @property feedUrl     URL of the channel/playlist to be scanned.
 * @property folderName  Sub-folder (inside the download root) where this channel's files are stored.
 * @property active      Whether the channel takes part in archiving runs.
 * @property lastChecked Moment of the last successful check. [Instant.EPOCH] means "never checked".
 * @property optionsJson Platform-specific options, serialized as JSON (e.g. `{"fetchShorts": true}`).
 * @throws IllegalArgumentException if [name], [platform], [feedUrl] or [folderName] is blank.
 */
data class Channel(
    val id: Int,
    val name: String,
    val platform: String,
    val feedUrl: String,
    val folderName: String,
    val active: Boolean,
    val lastChecked: Instant,
    val optionsJson: String
) {
    private val _videos = ArrayList<Video>()

    /** Read-only view of the videos currently loaded into this channel (empty until loaded). */
    val videos: List<Video> get() = Collections.unmodifiableList(_videos)

    init {
        ChannelValidator.validate(name, platform, feedUrl, folderName)
    }

    /**
     * Business rule: only videos from the same platform as the channel can belong to it
     * (a YouTube video cannot be moved into a Bilibili channel).
     *
     * @throws IllegalArgumentException if [video] comes from another platform.
     */
    fun ensureCanHold(video: Video) {
        require(video.platform == platform) {
            "A '${video.platform}' video cannot belong to the '$platform' channel '$name'"
        }
    }

    /**
     * Adds [video] to this channel's list.
     *
     * @throws IllegalArgumentException if the platform differs, if the video points to another
     *                                  channel id, or if a video with the same id is already in the list.
     */
    fun addVideo(video: Video) {
        ensureCanHold(video)
        require(video.channelId == id) { "Video '${video.videoId}' belongs to channel #${video.channelId}, not #$id" }
        require(_videos.none { it.videoId == video.videoId }) { "Video '${video.videoId}' is already in channel '$name'" }
        _videos.add(video)
    }

    /**
     * Computes how much of the cooldown period is still left for this channel.
     *
     * @param now             the reference moment.
     * @param cooldownMinutes the configured cooldown, in minutes.
     * @return the remaining time, or [Duration.ZERO] if the cooldown has already expired.
     */
    fun getRemainingCooldown(now: Instant, cooldownMinutes: Int): Duration {
        val cooldownDuration = Duration.ofMinutes(cooldownMinutes.toLong())
        val expirationTime = lastChecked.plus(cooldownDuration)

        if (now.isBefore(expirationTime)) {
            return Duration.between(now, expirationTime)
        }

        return Duration.ZERO
    }
}

/**
 * Data needed to register a channel that does not exist in the database yet
 * (so it has no `id` nor `lastChecked`).
 *
 * @property name        Display name of the channel.
 * @property platform    Source platform identifier (e.g. `youtube`).
 * @property feedUrl     URL of the channel/playlist to be scanned.
 * @property folderName  Sub-folder (inside the download root) where the files will be stored.
 * @property active      Whether the channel starts enabled. Defaults to `true`.
 * @property optionsJson Platform-specific options as JSON. Defaults to an empty object.
 * @throws IllegalArgumentException if [name], [platform], [feedUrl] or [folderName] is blank.
 */
data class NewChannel(
    val name: String,
    val platform: String,
    val feedUrl: String,
    val folderName: String,
    val active: Boolean = true,
    val optionsJson: String = "{}"
) {
    init {
        ChannelValidator.validate(name, platform, feedUrl, folderName)
    }
}
