package com.kelmy.mediaarchiver.infrastructure.persistence

import java.sql.SQLException
import java.time.Instant

import org.slf4j.LoggerFactory

import com.kelmy.mediaarchiver.core.domain.model.Channel
import com.kelmy.mediaarchiver.core.domain.model.NewChannel
import com.kelmy.mediaarchiver.core.domain.port.ChannelsRepository
import java.sql.ResultSet
import java.sql.Statement

/**
 * SQLite implementation of [ChannelsRepository], backed by the `channels` table.
 *
 * Timestamps are stored as ISO-8601 text (see [Instant.toString]).
 */
class SqlChannels : ChannelsRepository {
    companion object {
        private val log = LoggerFactory.getLogger(SqlChannels::class.java)

        private const val COLUMNS =
            "id, name, platform, feed_url, folder_name, active, last_checked, options_json"
    }

    override fun getChannels(): List<Channel> =
        queryChannels("SELECT $COLUMNS FROM channels", "Error querying channels from database")

    override fun getActiveChannels(): List<Channel> =
        queryChannels("SELECT $COLUMNS FROM channels WHERE active = 1;", "Error querying active channels from database")

    override fun insert(channel: NewChannel): Channel {
        val sql = """
            INSERT INTO channels (name, platform, feed_url, folder_name, active, last_checked, options_json)
            VALUES (?, ?, ?, ?, ?, ?, ?);
        """.trimIndent()

        val lastChecked = Instant.EPOCH

        return SqlExecutor.execute(log, "Error inserting channel '${channel.name}'") { conn ->
            conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
                stmt.setString(1, channel.name)
                stmt.setString(2, channel.platform)
                stmt.setString(3, channel.feedUrl)
                stmt.setString(4, channel.folderName)
                stmt.setBoolean(5, channel.active)
                stmt.setString(6, lastChecked.toString())
                stmt.setString(7, channel.optionsJson)
                stmt.executeUpdate()

                val id = stmt.generatedKeys.use { keys ->
                    check(keys.next()) { "Insert did not return a generated id" }
                    keys.getInt(1)
                }

                Channel(
                    id = id,
                    name = channel.name,
                    platform = channel.platform,
                    feedUrl = channel.feedUrl,
                    folderName = channel.folderName,
                    active = channel.active,
                    lastChecked = lastChecked,
                    optionsJson = channel.optionsJson
                )
            }
        }
    }

    override fun existsByFeedUrl(feedUrl: String): Boolean {
        val sql = "SELECT 1 FROM channels WHERE feed_url = ? LIMIT 1;"

        return SqlExecutor.execute(log, "Error checking if channel exists for '$feedUrl'") { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, feedUrl)
                stmt.executeQuery().use { rs -> rs.next() }
            }
        }
    }

    /**
     * Updates `last_checked` for all [channels] in a single transaction (JDBC batch):
     * either every row is updated or none is.
     */
    override fun updateLastChecked(channels: List<Channel>, timestamp: Instant) {
        if (channels.isEmpty()) return

        val sql = "UPDATE channels SET last_checked = ? WHERE id = ?;"

        SqlExecutor.execute(log, "Error updating last_checked in batch") { conn ->
            conn.autoCommit = false

            try {
                conn.prepareStatement(sql).use { stmt ->
                    for (channel in channels) {
                        stmt.setString(1, timestamp.toString())
                        stmt.setInt(2, channel.id)
                        stmt.addBatch()
                    }
                    stmt.executeBatch()
                }
                conn.commit()
                log.info("last_checked updated for {} channels in a batch.", channels.size)
            } catch (e: SQLException) {
                conn.rollback()
                throw e
            }
        }
    }

    // TODO: not implemented yet (the GUI already calls it).
    override fun deleteChannel(id: Int): Boolean {
        TODO("Not yet implemented")
    }

    // TODO: not implemented yet (the GUI already calls it).
    override fun updateChannel(channel: Channel): Boolean {
        TODO("Not yet implemented")
    }

    /**
     * Runs a `SELECT` that returns channel rows (all of [COLUMNS]) and maps every row to a [Channel].
     */
    private fun queryChannels(sql: String, errorMessage: String): List<Channel> =
        SqlExecutor.execute(log, errorMessage) { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    buildList<Channel> {
                        while (rs.next()) add(mapRow(rs))
                    }
                }
            }
        }

    /** Maps the current row of [rs] to a [Channel]. A missing `last_checked` becomes [Instant.EPOCH]. */
    private fun mapRow(rs: ResultSet): Channel {
        return Channel(
            id = rs.getInt("id"),
            name = rs.getString("name"),
            platform = rs.getString("platform"),
            feedUrl = rs.getString("feed_url"),
            folderName = rs.getString("folder_name"),
            active = rs.getBoolean("active"),
            lastChecked = rs.getString("last_checked")?.let(Instant::parse) ?: Instant.EPOCH,
            optionsJson = rs.getString("options_json")
        )
    }
}
