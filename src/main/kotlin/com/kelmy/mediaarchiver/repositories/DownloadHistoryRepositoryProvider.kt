package com.kelmy.mediaarchiver.repositories

import com.kelmy.mediaarchiver.model.DownloadHistoryModel

interface DownloadHistoryRepositoryProvider {
    /**
     * return true if video is already in database
     */
    fun exists(platform: String, videoId: String): Boolean

    /**
     * save a video to database
     * return true if no error occurs
     */
    fun save(model: DownloadHistoryModel): Boolean
}