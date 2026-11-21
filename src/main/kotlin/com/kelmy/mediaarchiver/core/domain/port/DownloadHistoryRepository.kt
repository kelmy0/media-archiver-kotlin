package com.kelmy.mediaarchiver.core.domain.port

import com.kelmy.mediaarchiver.core.domain.model.DownloadHistory

interface DownloadHistoryRepository {
    /**
     * return true if video is already in database
     */
    fun exists(platform: String, videoId: String): Boolean

    /**
     * save a video to database
     * return true if no error occurs
     */
    fun save(model: DownloadHistory): Boolean
}