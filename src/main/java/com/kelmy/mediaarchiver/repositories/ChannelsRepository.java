package com.kelmy.mediaarchiver.repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kelmy.mediaarchiver.core.ChannelsRepositoryProvider;
import com.kelmy.mediaarchiver.database.ConnectionFactory;
import com.kelmy.mediaarchiver.schemas.BaseChannelConfig;

public final class ChannelsRepository implements ChannelsRepositoryProvider {
    private static final Logger log = LoggerFactory.getLogger(ChannelsRepository.class);

    @Override
    public List<BaseChannelConfig> getActiveChannels() {
        List<BaseChannelConfig> channels = new ArrayList<>();
        String sql = "SELECT id, name, platform, feed_url, folder_name, active, last_checked, options_json FROM channels WHERE active = 1";

        try (Connection conn = ConnectionFactory.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Integer id = rs.getInt("id");
                String name = rs.getString("name");
                String platform = rs.getString("platform");
                String folderName = rs.getString("folder_name");
                String feedUrl = rs.getString("feed_url");
                Boolean active = rs.getBoolean("active");
                String optionsJson = rs.getString("options_json");
                Instant lastChecked = rs.getTimestamp("last_checked").toInstant();

                BaseChannelConfig channel = new BaseChannelConfig(id, name, platform, feedUrl, folderName, active,
                        lastChecked, optionsJson);

                channels.add(channel);
            }
        } catch (SQLException e) {
            log.error("Error querying active channels from database", e);
            throw new RuntimeException("Error querying active channels from database", e);
        }

        return channels;
    }

    @Override
    public void updateLastChecked(List<BaseChannelConfig> channels, Instant timestamp) {
        if (channels == null || channels.isEmpty()) {
            return;
        }

        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp cant be null");
        }

        String sql = "UPDATE channels SET last_checked = ? WHERE id = ?";

        try (Connection conn = ConnectionFactory.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (BaseChannelConfig channel : channels) {
                    stmt.setString(1, timestamp.toString());
                    stmt.setString(2, channel.getName());
                    stmt.addBatch();
                }

                stmt.executeBatch();
                conn.commit();

                log.info("Selection timestamp successfully updated for {} channels in a batch.", channels.size());

            } catch (SQLException e) {
                conn.rollback();
                log.error("Error during execution of the update batch; transaction rolled back.", e);
                throw e;
            }
        } catch (SQLException e) {
            log.error("Error updating last_checked for channels.", e);
            throw new RuntimeException("Error updating last_checked in batch", e);
        }

    }

}
