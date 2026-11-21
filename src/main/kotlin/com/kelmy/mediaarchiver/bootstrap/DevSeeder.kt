package com.kelmy.mediaarchiver.bootstrap

import com.kelmy.mediaarchiver.core.domain.model.NewChannel
import com.kelmy.mediaarchiver.core.domain.port.ChannelsRepository
import org.slf4j.LoggerFactory
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Shape of the seed file (`seed.local.json`).
 *
 * @property channels the channels to be registered.
 */
data class SeedFile(val channels: List<SeedChannel> = emptyList())

/**
 * One channel entry of the seed file.
 *
 * @property name       display name of the channel.
 * @property platform   source platform (e.g. `youtube`).
 * @property feedUrl    URL of the channel/playlist to monitor. Used to detect duplicates.
 * @property folderName sub-folder where the channel's files are stored.
 * @property options    optional platform-specific options; stored as JSON text (`{}` when absent).
 */
data class SeedChannel(
    val name: String,
    val platform: String,
    val feedUrl: String,
    val folderName: String,
    val options: JsonNode? = null
)

/**
 * Development helper that registers the channels listed in `seed.local.json`
 * (see `seed.example.json`) in the database.
 *
 * It is idempotent: a channel whose `feedUrl` is already registered is skipped.
 *
 * @property channels repository where the channels are inserted.
 */
class DevSeeder(
    private val channels: ChannelsRepository
) {
    companion object {
        private val seedPath: Path = Paths.get("seed.local.json")
        private val objectMapper = jacksonObjectMapper()
        private val log = LoggerFactory.getLogger(DevSeeder::class.java)
    }

    /**
     * Reads the seed file and inserts the channels that do not exist yet.
     * If the file is missing, it only logs an error and returns.
     */
    fun run() {
        if (!Files.exists(seedPath)) {
            log.error("Seed file not found: $seedPath (copy seed.example.json and fill it in)")
            return
        }

        val seed = objectMapper.readValue(Files.readString(seedPath), SeedFile::class.java)

        var inserted = 0
        var skipped = 0

        seed.channels.forEach { item ->
            if (channels.existsByFeedUrl(item.feedUrl)) {
                log.info("Skipping '{}': feed already exists", item.name)
                skipped++
                return@forEach
            }

            channels.insert(
                NewChannel(
                    name = item.name,
                    platform = item.platform,
                    feedUrl = item.feedUrl,
                    folderName = item.folderName,
                    optionsJson = item.options?.toString() ?: "{}"
                )
            )
            inserted++
        }

        log.info("Seed finished: {} inserted, {} skipped", inserted, skipped)
    }
}