package com.kelmy.mediaarchiver.repositories

import java.sql.SQLException
import java.time.Instant
import java.util.ArrayList

import org.slf4j.LoggerFactory

import com.kelmy.mediaarchiver.database.ConnectionFactory
import com.kelmy.mediaarchiver.schemas.BaseChannelConfig

class ChannelsRepository : ChannelsRepositoryProvider {
    companion object {
        private val log = LoggerFactory.getLogger(ChannelsRepository::class.java)
    }


    override fun getActiveChannels(): List<BaseChannelConfig> {
        val channels = ArrayList<BaseChannelConfig>()
        val sql =
            "SELECT id, name, platform, feed_url, folder_name, active, last_checked, options_json FROM channels WHERE active = 1"

        try {
            ConnectionFactory.getConnection().use { conn ->
                conn.prepareStatement(sql).use { stmt ->
                    stmt.executeQuery().use { rs ->
                        while (rs.next()) {
                            channels.add(
                                BaseChannelConfig(
                                    id = rs.getInt("id"),
                                    name = rs.getString("name"),
                                    platform = rs.getString("platform"),
                                    feedUrl = rs.getString("feed_url"),
                                    folderName = rs.getString("folder_name"),
                                    active = rs.getBoolean("active"),
                                    lastChecked = rs.getTimestamp("last_checked")?.toInstant() ?: Instant.EPOCH,
                                    optionsJson = rs.getString("options_json")
                                )
                            )
                        }
                    }

                }
            }
        } catch (e: SQLException) {
            log.error("Error querying active channels from database", e)
            throw RuntimeException("Error querying active channels from database", e)
        }

        return channels
    }


    override fun updateLastChecked(channels: List<BaseChannelConfig>, timestamp: Instant) {
        if (channels.isEmpty()) {
            return
        }

        val sql = "UPDATE channels SET last_checked = ? WHERE id = ?"

        try {
            ConnectionFactory.getConnection().use { conn ->
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
                    log.info("Selection timestamp successfully updated for {} channels in a batch.", channels.size)
                } catch (e: SQLException) {
                    conn.rollback()
                    log.error("Error during execution of the update batch transaction rolled back.", e)
                    throw e
                }

            }
        } catch (e: SQLException) {
            log.error("Error updating last_checked for channels.", e)
            throw RuntimeException("Error updating last_checked in batch", e)
        }
    }

}
