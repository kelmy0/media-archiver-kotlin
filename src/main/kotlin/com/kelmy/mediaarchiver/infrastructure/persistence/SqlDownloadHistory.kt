package com.kelmy.mediaarchiver.infrastructure.persistence

import com.kelmy.mediaarchiver.core.domain.model.DownloadHistory
import com.kelmy.mediaarchiver.core.domain.port.DownloadHistoryRepository
import org.slf4j.LoggerFactory

/**
 * SQLite implementation of [DownloadHistoryRepository], backed by the `download_history` table.
 */
class SqlDownloadHistory : DownloadHistoryRepository {
    companion object {
        private val log = LoggerFactory.getLogger(SqlDownloadHistory::class.java)
    }

    override fun exists(platform: String, videoId: String): Boolean {
        val sql = "SELECT 1 FROM download_history WHERE platform = ? AND video_id = ? LIMIT 1;"

        return SqlExecutor.execute(
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

    override fun save(model: DownloadHistory): Boolean {
        val sql = """
        INSERT INTO download_history (platform, video_id, file_path, source_url, downloaded_at, archive_status)
        VALUES (?, ?, ?, ?, ?, ?)
    """.trimIndent()

        val rowsAffected = SqlExecutor.execute(
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
