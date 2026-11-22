package com.kelmy.mediaarchiver.library.infrastructure.persistence

import com.kelmy.mediaarchiver.library.domain.model.ArchiveStatus
import com.kelmy.mediaarchiver.library.domain.model.Video
import com.kelmy.mediaarchiver.library.domain.port.VideoRepository
import com.kelmy.mediaarchiver.shared.infrastructure.persistence.SqlExecutor
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.time.Instant

/**
 * SQLite implementation of [VideoRepository], backed by the `videos` table.
 *
 * Timestamps are stored as ISO-8601 text (see [Instant.toString]).
 */
internal class SqlVideos : VideoRepository {
    companion object {
        private val log = LoggerFactory.getLogger(SqlVideos::class.java)

        /**
         * Maximum number of ids per query. Each id is one `?` parameter, and older SQLite builds limit a
         * statement to 999 parameters (one is used by `platform`), so 500 is safe everywhere.
         */
        private const val MAX_IDS_PER_QUERY = 500

        private const val COLUMNS =
            "platform, video_id, channel_id, title, file_path, source_url, downloaded_at, archive_status, archive_url"
    }

    override fun exists(platform: String, videoId: String): Boolean {
        val sql = "SELECT 1 FROM videos WHERE platform = ? AND video_id = ? LIMIT 1;"

        return SqlExecutor.execute(
            log,
            "Error checking if video is registered. platform: '$platform', videoId: '$videoId'."
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
            "Error checking which videos are already registered. platform: '$platform'."
        ) { conn ->
            val found = HashSet<String>()

            for (block in videoIds.chunked(MAX_IDS_PER_QUERY)) {
                val placeholders = block.joinToString(",") { "?" }
                val sql = "SELECT video_id FROM videos WHERE platform = ? AND video_id IN ($placeholders);"

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

    override fun find(platform: String, videoId: String): Video? {
        val sql = "SELECT $COLUMNS FROM videos WHERE platform = ? AND video_id = ?;"

        return SqlExecutor.execute(log, "Error finding video '$videoId' ($platform)") { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, platform)
                stmt.setString(2, videoId)
                stmt.executeQuery().use { rs -> if (rs.next()) mapRow(rs) else null }
            }
        }
    }

    override fun findByChannel(channelId: Int): List<Video> {
        val sql = "SELECT $COLUMNS FROM videos WHERE channel_id = ? ORDER BY downloaded_at DESC;"

        return SqlExecutor.execute(log, "Error listing videos of channel #$channelId") { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, channelId)
                stmt.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(mapRow(rs)) }
                }
            }
        }
    }

    override fun insert(video: Video): Boolean {
        val sql = """
            INSERT INTO videos (platform, video_id, channel_id, title, file_path, source_url,
                                downloaded_at, archive_status, archive_url)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        return SqlExecutor.execute(log, "Error saving video (${video.videoId}) to the database.") { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, video.platform)
                stmt.setString(2, video.videoId)
                stmt.setInt(3, video.channelId)
                stmt.setString(4, video.title)
                stmt.setString(5, video.filePath)
                stmt.setString(6, video.sourceUrl)
                stmt.setString(7, video.downloadedAt.toString())
                stmt.setString(8, video.archiveStatus.value)
                stmt.setString(9, video.archiveUrl)
                stmt.executeUpdate() > 0
            }
        }
    }

    override fun update(video: Video): Boolean {
        val sql = "UPDATE videos SET channel_id = ?, title = ?, file_path = ? WHERE platform = ? AND video_id = ?;"

        return SqlExecutor.execute(log, "Error updating video (${video.videoId})") { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, video.channelId)
                stmt.setString(2, video.title)
                stmt.setString(3, video.filePath)
                stmt.setString(4, video.platform)
                stmt.setString(5, video.videoId)
                stmt.executeUpdate() > 0
            }
        }
    }

    override fun delete(platform: String, videoId: String): Boolean {
        val sql = "DELETE FROM videos WHERE platform = ? AND video_id = ?;"

        return SqlExecutor.execute(log, "Error deleting video ($videoId)") { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, platform)
                stmt.setString(2, videoId)
                stmt.executeUpdate() > 0
            }
        }
    }

    /** Maps the current row of [rs] (all of [COLUMNS]) to a [Video]. */
    private fun mapRow(rs: ResultSet): Video = Video(
        platform = rs.getString("platform"),
        videoId = rs.getString("video_id"),
        channelId = rs.getInt("channel_id"),
        title = rs.getString("title"),
        filePath = rs.getString("file_path"),
        sourceUrl = rs.getString("source_url"),
        downloadedAt = Instant.parse(rs.getString("downloaded_at")),
        archiveStatus = ArchiveStatus.fromValue(rs.getString("archive_status") ?: "none"),
        archiveUrl = rs.getString("archive_url")
    )
}
