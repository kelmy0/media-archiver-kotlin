package com.kelmy.mediaarchiver.repositories

import java.sql.SQLException
import java.time.Instant

import org.slf4j.LoggerFactory

import com.kelmy.mediaarchiver.model.ChannelModel
import com.kelmy.mediaarchiver.model.NewChannel
import com.kelmy.mediaarchiver.schemas.ChannelSchema
import java.sql.ResultSet
import java.sql.Statement

class ChannelsRepository : ChannelsRepositoryProvider {
    companion object {
        private val log = LoggerFactory.getLogger(ChannelsRepository::class.java)

        private const val COLUMNS =
            "id, name, platform, feed_url, folder_name, active, last_checked, options_json"
    }

    override fun getChannels(): List<ChannelModel> {
        val sql = "SELECT $COLUMNS FROM channels"

        return RepoHelpers.execute(log, "Error querying channels from database") { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    buildList<ChannelModel> {
                        while (rs.next()) add(mapRow(rs))
                    }
                }
            }
        }
    }

    override fun getActiveChannels(): List<ChannelModel> {
        val sql = "SELECT $COLUMNS FROM channels WHERE active = 1;"

        return RepoHelpers.execute(log, "Error querying active channels from database") { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    buildList<ChannelModel> {
                        while (rs.next()) add(mapRow(rs))
                    }
                }
            }
        }
    }

    override fun insert(channel: NewChannel): ChannelModel {
        val sql = """
            INSERT INTO channels (name, platform, feed_url, folder_name, active, last_checked, options_json)
            VALUES (?, ?, ?, ?, ?, ?, ?);
        """.trimIndent()

        val lastChecked = Instant.EPOCH

        return RepoHelpers.execute(log, "Error inserting channel '${channel.name}'") { conn ->
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

                ChannelModel(
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

        return RepoHelpers.execute(log, "Error checking if channel exists for '$feedUrl'") { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, feedUrl)
                stmt.executeQuery().use { rs -> rs.next() }
            }
        }
    }

    override fun updateLastChecked(channels: List<ChannelModel>, timestamp: Instant) {
        if (channels.isEmpty()) return

        val sql = "UPDATE channels SET last_checked = ? WHERE id = ?;"

        RepoHelpers.execute(log, "Error updating last_checked in batch") { conn ->
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

    override fun deleteChannel(id: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun updateChannel(channel: ChannelModel): Boolean {
        TODO("Not yet implemented")
    }

    private fun mapRow(rs: ResultSet): ChannelModel {
        val schema = ChannelSchema(
            id = rs.getInt("id"),
            name = rs.getString("name"),
            platform = rs.getString("platform"),
            feedUrl = rs.getString("feed_url"),
            folderName = rs.getString("folder_name"),
            active = rs.getBoolean("active"),
            lastChecked = rs.getString("last_checked")?.let(Instant::parse) ?: Instant.EPOCH,
            optionsJson = rs.getString("options_json")
        )

        return ChannelModel(
            id = schema.id,
            name = schema.name,
            platform = schema.platform,
            feedUrl = schema.feedUrl,
            folderName = schema.folderName,
            active = schema.active,
            lastChecked = schema.lastChecked,
            optionsJson = schema.optionsJson
        )
    }
}
