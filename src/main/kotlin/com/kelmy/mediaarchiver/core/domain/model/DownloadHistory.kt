package com.kelmy.mediaarchiver.core.domain.model

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
 * Record of an item that was downloaded. The pair ([platform], [videoId]) identifies it
 * and is what prevents the same media from being downloaded twice.
 *
 * @property platform      Platform the item came from (e.g. `youtube`).
 * @property videoId       Identifier of the item on that platform.
 * @property filePath      Absolute path of the generated `.mkv` file.
 * @property sourceUrl     URL the item was downloaded from.
 * @property downloadedAt  Moment the download finished.
 * @property archiveStatus Status of the copy in an external archive. Defaults to [ArchiveStatus.NONE].
 * @property archiveUrl    URL of the archived copy, when there is one.
 */
data class DownloadHistory(
    val platform: String,
    val videoId: String,
    val filePath: String,
    val sourceUrl: String,
    val downloadedAt: Instant,
    val archiveStatus: ArchiveStatus = ArchiveStatus.NONE,
    val archiveUrl: String? = null
)
