package com.kelmy.mediaarchiver.repositories

import com.kelmy.mediaarchiver.model.DownloadHistoryModel
import org.slf4j.LoggerFactory

class DownloadHistoryRepository : DownloadHistoryRepositoryProvider {
    companion object {
        private val log = LoggerFactory.getLogger(DownloadHistoryRepository::class.java)
    }

    override fun exists(platform: String, videoId: String): Boolean {
        val sql = "SELECT platform, video_id from download_history WHERE platform = ? AND video_id = ? LIMIT 1;"

        return RepoHelpers.execute(
            log,
            "Error checking if video was already downloaded. platform: '$platform', videoId: '$videoId'."
        ) { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, platform)
                stmt.setString(2, videoId)
                stmt.executeQuery().use { rs -> rs.next() }
            }
        }
    }

    override fun save(model: DownloadHistoryModel): Boolean {
        val sql = """
        INSERT INTO download_history (platform, video_id, file_path, source_url, downloaded_at, archive_status)
        VALUES (?, ?, ?, ?, ?, ?)
    """.trimIndent()

        val rowsAffected = RepoHelpers.execute(
            log,
            "error saving video (${model.videoId}) to the database."
        ) { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, model.platform)
                stmt.setString(2, model.videoId)
                stmt.setString(3, model.filePath)
                stmt.setString(4, model.sourceUrl)
                stmt.setString(5, model.downloadedAt.toString())
                stmt.setString(6, model.archiveStatus.value)

                stmt.executeUpdate()
            }
        }

        return rowsAffected > 0
    }
}