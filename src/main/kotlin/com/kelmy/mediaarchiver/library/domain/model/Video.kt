package com.kelmy.mediaarchiver.library.domain.model

import java.time.Instant

/**
 * State of the copy of a downloaded item in an external archive (e.g. the Internet Archive).
 *
 * @property value Lower-case representation stored in the database.
 */
enum class ArchiveStatus(val value: String) {
    /** The item was not sent to any external archive. */
    NONE("none"),

    /** The item is waiting to be sent. */
    PENDING("pending"),

    /** The item was successfully archived. */
    ARCHIVED("archived"),

    /** The attempt to archive the item failed. */
    FAILED("failed"),

    /** The item was intentionally not archived. */
    SKIPPED("skipped");

    companion object {
        /**
         * Converts the database representation back into an [ArchiveStatus] (case-insensitive).
         *
         * @throws IllegalArgumentException if [value] does not match any status.
         */
        fun fromValue(value: String): ArchiveStatus =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown status: $value")
    }
}

/**
 * A video that belongs to a [Channel] and is already stored (or registered) on the local disk.
 *
 * It replaces the old "download history" record: the pair ([platform], [videoId]) still identifies
 * the item and is what prevents the same media from being downloaded twice, but the record now also
 * carries a [title] and the [channelId] of the channel that owns it.
 *
 * Relationship with [Channel]: **aggregation**. A channel holds a list of videos, but a video is
 * independent of any particular channel: it can be moved to another channel of the same platform
 * (see [Channel.ensureCanHold]). The video points back to its channel by id (not by object) to avoid
 * a circular object graph.
 *
 * Instances are immutable; to edit one use [copy], which runs the validation again.
 *
 * @property platform      Platform the item came from (e.g. `youtube`).
 * @property videoId       Identifier of the item on that platform. Never changes.
 * @property channelId     Id of the [Channel] this video belongs to.
 * @property title         Human readable title. Editable.
 * @property filePath      Absolute path of the video file. Blank means "file location unknown".
 * @property sourceUrl     URL the item was downloaded from. Never changes (the [videoId] derives from it).
 * @property downloadedAt  Moment the download finished (or the video was registered).
 * @property archiveStatus Status of the copy in an external archive. Defaults to [ArchiveStatus.NONE].
 * @property archiveUrl    URL of the archived copy, when there is one.
 * @throws IllegalArgumentException if [platform], [videoId], [title] or [sourceUrl] is blank,
 *                                  or if [channelId] is not positive.
 */
data class Video(
    val platform: String,
    val videoId: String,
    val channelId: Int,
    val title: String,
    val filePath: String,
    val sourceUrl: String,
    val downloadedAt: Instant,
    val archiveStatus: ArchiveStatus = ArchiveStatus.NONE,
    val archiveUrl: String? = null
) {
    init {
        require(platform.isNotBlank()) { "platform cant be blank" }
        require(videoId.isNotBlank()) { "videoId cant be blank" }
        require(channelId > 0) { "channelId must be positive" }
        require(title.isNotBlank()) { "title cant be blank" }
        require(sourceUrl.isNotBlank()) { "sourceUrl cant be blank" }
    }
}
