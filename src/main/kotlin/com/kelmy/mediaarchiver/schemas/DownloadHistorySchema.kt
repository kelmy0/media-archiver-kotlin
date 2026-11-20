package com.kelmy.mediaarchiver.schemas

import com.kelmy.mediaarchiver.model.ArchiveStatus
import java.time.Instant

data class DownloadHistorySchema(
    val platform: String,
    val videoId: String,
    val filePath: String,
    val sourceUrl: String,
    val downloadedAt: Instant,
    val archiveStatus: ArchiveStatus = ArchiveStatus.NONE,
    val archiveUrl: String? = null
)
