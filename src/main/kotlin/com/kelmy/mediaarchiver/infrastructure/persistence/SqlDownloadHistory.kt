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

        /**
         * Maximum number of ids per query. Each id is one `?` parameter, and older SQLite builds limit a
         * statement to 999 parameters (one is used by `platform`), so 500 is safe everywhere.
         */
        private const val MAX_IDS_PER_QUERY = 500
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

    /**
     * Runs a read-only `SELECT ... IN (...)` per block of [MAX_IDS_PER_QUERY] ids, all on a single
     * connection. The query is covered by the primary key `(platform, video_id)`. Only `?` placeholders
     * are concatenated into the SQL text; the ids themselves are always bound as parameters.
     */
    override fun findExistingIds(platform: String, videoIds: Collection<String>): Set<String> {
        if (videoIds.isEmpty()) return emptySet()

        return SqlExecutor.execute(
            log,
            "Error checking which videos were already downloaded. platform: '$platform'."
        ) { conn ->
            val found = HashSet<String>()

            for (block in videoIds.chunked(MAX_IDS_PER_QUERY)) {
                val placeholders = block.joinToString(",") { "?" }
                val sql = "SELECT video_id FROM download_history WHERE platform = ? AND video_id IN ($placeholders);"

                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, platform)
                    block.forEachIndexed { index, id -> stmt.setString(index + 2, id) }
                    stmt.executeQuery().use { rs ->
                        while (rs.next()) found.add(rs.getString(1))
                    }
                }
            }

            found
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
