package com.kelmy.mediaarchiver.library.domain.port

import com.kelmy.mediaarchiver.library.domain.model.Video

/**
 * Port for the videos of the library. It is also the source of truth used to avoid
 * downloading the same media twice (see [findExistingIds]).
 */
interface VideoRepository {
    /**
     * Checks whether a video is already registered.
     *
     * @param platform platform identifier (e.g. `youtube`).
     * @param videoId  identifier of the item on that platform.
     */
    fun exists(platform: String, videoId: String): Boolean

    /**
     * Tells which of the given items are already registered, using as few queries as possible.
     * Prefer it over calling [exists] in a loop.
     *
     * @param platform platform identifier (e.g. `youtube`).
     * @param videoIds identifiers to look up. May be large; implementations split it as needed.
     * @return the subset of [videoIds] that is already registered (empty if none, or if [videoIds] is empty).
     */
    fun findExistingIds(platform: String, videoIds: Collection<String>): Set<String>

    /** Returns the video identified by ([platform], [videoId]), or `null` if there is none. */
    fun find(platform: String, videoId: String): Video?

    /** Returns every video of the channel [channelId], ordered by download date (newest first). */
    fun findByChannel(channelId: Int): List<Video>

    /**
     * Stores a new video.
     *
     * @return `true` if the record was stored.
     * @throws RuntimeException if a video with the same ([Video.platform], [Video.videoId]) already exists.
     */
    fun insert(video: Video): Boolean

    /**
     * Updates the editable fields of an existing video (title, channel and file path).
     * The platform, the id and the source URL are the video's identity and are never changed.
     *
     * @return `true` if a video was updated.
     */
    fun update(video: Video): Boolean

    /**
     * Removes the record of a video (the file on disk is never touched).
     *
     * @return `true` if a video was deleted.
     */
    fun delete(platform: String, videoId: String): Boolean
}
