package com.kelmy.mediaarchiver.bootstrap

import com.kelmy.mediaarchiver.core.domain.model.NewChannel
import com.kelmy.mediaarchiver.core.domain.port.ChannelsRepository
import org.slf4j.LoggerFactory
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/** Shape of the seed file */
data class SeedFile(val channels: List<SeedChannel> = emptyList())

data class SeedChannel(
    val name: String,
    val platform: String,
    val feedUrl: String,
    val folderName: String,
    val options: JsonNode? = null
)

class DevSeeder(
    private val channels: ChannelsRepository
) {
    companion object {
        private val seedPath: Path = Paths.get("seed.local.json")
        private val objectMapper = jacksonObjectMapper()
        private val log = LoggerFactory.getLogger(DevSeeder::class.java)
    }

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