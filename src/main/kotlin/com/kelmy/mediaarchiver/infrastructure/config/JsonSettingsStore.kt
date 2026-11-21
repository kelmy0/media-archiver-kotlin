package com.kelmy.mediaarchiver.infrastructure.config

import com.kelmy.mediaarchiver.core.domain.model.AppSettings
import com.kelmy.mediaarchiver.core.domain.port.SettingsStore
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import org.slf4j.LoggerFactory

import tools.jackson.module.kotlin.jacksonObjectMapper
import java.nio.charset.StandardCharsets
import java.nio.file.StandardOpenOption
import kotlin.concurrent.Volatile

/**
 * [SettingsStore] that keeps the settings in a JSON file (`config.local.json`, relative to the
 * working directory).
 *
 * The file is read **once**, when the object is created, and the result is cached in memory.
 * If the file does not exist it is created with default values; if it cannot be read or is
 * invalid, the defaults are used (and the error is logged) without touching the file.
 */
class JsonSettingsStore : SettingsStore {
    companion object {
        private val CONFIG_PATH: Path = Paths.get("config.local.json")
        private val objectMapper = jacksonObjectMapper()
        private val log = LoggerFactory.getLogger(JsonSettingsStore::class.java)
    }

    @Volatile
    private var cachedConfig: AppSettings = loadOrCreateConfig()

    override fun current(): AppSettings = cachedConfig

    /**
     * Loads the settings from disk, creating the file with default values when it is missing.
     *
     * @return the loaded settings, or the defaults if anything goes wrong.
     */
    private fun loadOrCreateConfig(): AppSettings {
        return try {
            if (!Files.exists(CONFIG_PATH)) {
                log.info("config.local.json not found. Generating a new one...")

                val config = AppSettingsSchema()
                val jsonStr = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config)
                Files.writeString(CONFIG_PATH, jsonStr)

                log.info("config.local.json created successfully at: {}", CONFIG_PATH.toAbsolutePath())
                config.toModel()
            } else {
                val content = Files.readString(CONFIG_PATH)
                val config = objectMapper.readValue(content, AppSettingsSchema::class.java)
                log.info("config.local.json loaded successfully!")
                config.toModel()
            }
        } catch (e: Exception) {
            log.error("Fail loading configuration: {}", e.message)
            AppSettings()
        }
    }

    /**
     * Writes [settings] to the JSON file (replacing its content) and updates the in-memory cache.
     * The cache is updated with the object that was just written, so the file is not read back.
     *
     * @throws java.io.IOException if the file cannot be written.
     */
    override fun save(settings: AppSettings) {
        val json =
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(AppSettingsSchema.fromModel(settings))

        CONFIG_PATH.toAbsolutePath().parent?.let { Files.createDirectories(it) }
        Files.writeString(
            CONFIG_PATH,
            json,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        )
        cachedConfig = settings
        log.info("Configuration saved to ${CONFIG_PATH.toAbsolutePath()}")
    }
}
