package com.kelmy.mediaarchiver.core.domain.model

import java.time.Instant

enum class ArchiveStatus(val value: String) {
    NONE("none"),
    PENDING("pending"),
    ARCHIVED("archived"),
    FAILED("failed"),
    SKIPPED("skipped");

    companion object {
        fun fromValue(value: String): ArchiveStatus =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown status: $value")
    }
}

data class DownloadHistory(
    val platform: String,
    val videoId: String,
    val filePath: String,
    val sourceUrl: String,
    val downloadedAt: Instant,
    val archiveStatus: ArchiveStatus = ArchiveStatus.NONE,
    val archiveUrl: String? = null
)