package com.kelmy.mediaarchiver.backup.infrastructure

import com.fasterxml.jackson.annotation.JsonProperty
import com.kelmy.mediaarchiver.backup.domain.port.BackupStore
import com.kelmy.mediaarchiver.library.domain.model.ArchiveStatus
import com.kelmy.mediaarchiver.library.domain.model.Channel
import com.kelmy.mediaarchiver.library.domain.model.Video
import tools.jackson.core.JacksonException
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.format.DateTimeParseException

/** JSON shape of a backup file. Kept out of the domain, like `AppSettingsSchema`. */
internal data class BackupFileSchema(
    @JsonProperty("version")
    val version: Int = 1,

    @JsonProperty("channels")
    val channels: List<ChannelEntry> = emptyList()
) {
    /** One channel and the videos it owns (the aggregate is stored as a nested list). */
    data class ChannelEntry(
        @JsonProperty("name") val name: String = "",
        @JsonProperty("platform") val platform: String = "",
        @JsonProperty("feed_url") val feedUrl: String = "",
        @JsonProperty("folder_name") val folderName: String = "",
        @JsonProperty("active") val active: Boolean = true,
        @JsonProperty("options_json") val optionsJson: String = "{}",
        @JsonProperty("videos") val videos: List<VideoEntry> = emptyList()
    )

    /** One video; the channel is implied by the entry that contains it. */
    data class VideoEntry(
        @JsonProperty("video_id") val videoId: String = "",
        @JsonProperty("title") val title: String = "",
        @JsonProperty("file_path") val filePath: String = "",
        @JsonProperty("source_url") val sourceUrl: String = "",
        @JsonProperty("downloaded_at") val downloadedAt: String = "",
        @JsonProperty("archive_status") val archiveStatus: String = "none",
        @JsonProperty("archive_url") val archiveUrl: String? = null
    )
}

/**
 * [BackupStore] that keeps the backup as a single pretty-printed JSON file.
 */
internal class JsonBackupStore : BackupStore {
    private companion object {
        val objectMapper = jacksonObjectMapper()
    }

    override fun write(target: Path, channels: List<Channel>) {
        val schema = BackupFileSchema(
            channels = channels.map { channel ->
                BackupFileSchema.ChannelEntry(
                    name = channel.name,
                    platform = channel.platform,
                    feedUrl = channel.feedUrl,
                    folderName = channel.folderName,
                    active = channel.active,
                    optionsJson = channel.optionsJson,
                    videos = channel.videos.map { video ->
                        BackupFileSchema.VideoEntry(
                            videoId = video.videoId,
                            title = video.title,
                            filePath = video.filePath,
                            sourceUrl = video.sourceUrl,
                            downloadedAt = video.downloadedAt.toString(),
                            archiveStatus = video.archiveStatus.value,
                            archiveUrl = video.archiveUrl
                        )
                    }
                )
            }
        )

        target.toAbsolutePath().parent?.let { Files.createDirectories(it) }
        Files.writeString(
            target,
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema),
            StandardCharsets.UTF_8
        )
    }

    override fun read(source: Path): List<Channel> {
        val schema = try {
            objectMapper.readValue(Files.readString(source, StandardCharsets.UTF_8), BackupFileSchema::class.java)
        } catch (e: JacksonException) {
            throw IllegalArgumentException("Not a valid backup file: ${e.message}", e)
        }

        return try {
            toChannels(schema)
        } catch (e: DateTimeParseException) {
            throw IllegalArgumentException("Invalid date in the backup file: ${e.message}", e)
        }
    }

    /** Rebuilds the domain objects (a channel with its list of videos) from the file's content. */
    private fun toChannels(schema: BackupFileSchema): List<Channel> =
        schema.channels.map { entry ->
            // The id is irrelevant here (the importer creates or finds the real channel); any positive value works.
            val channel = Channel(
                id = 1,
                name = entry.name,
                platform = entry.platform,
                feedUrl = entry.feedUrl,
                folderName = entry.folderName,
                active = entry.active,
                lastChecked = Instant.EPOCH,
                optionsJson = entry.optionsJson
            )
            entry.videos.forEach { v ->
                channel.addVideo(
                    Video(
                        platform = entry.platform,
                        videoId = v.videoId,
                        channelId = channel.id,
                        title = v.title,
                        filePath = v.filePath,
                        sourceUrl = v.sourceUrl,
                        downloadedAt = Instant.parse(v.downloadedAt),
                        archiveStatus = ArchiveStatus.fromValue(v.archiveStatus),
                        archiveUrl = v.archiveUrl
                    )
                )
            }
            channel
        }
}
