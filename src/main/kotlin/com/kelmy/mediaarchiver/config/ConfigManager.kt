package com.kelmy.mediaarchiver.config

import com.kelmy.mediaarchiver.model.AppSettingsModel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import org.slf4j.LoggerFactory

import com.kelmy.mediaarchiver.schemas.AppSettingsSchema

import tools.jackson.module.kotlin.jacksonObjectMapper
import java.nio.charset.StandardCharsets
import java.nio.file.StandardOpenOption
import kotlin.concurrent.Volatile

class ConfigManager : ConfigProvider {
    companion object {
        private val CONFIG_PATH: Path = Paths.get("config.local.json")
        private val objectMapper = jacksonObjectMapper()
        private val log = LoggerFactory.getLogger(ConfigManager::class.java)
    }

    @Volatile
    private var cachedConfig: AppSettingsSchema = loadOrCreateConfig()

    override fun getConfigs(): AppSettingsSchema = cachedConfig

    private fun loadOrCreateConfig(): AppSettingsSchema {
        return try {
            if (!Files.exists(CONFIG_PATH)) {
                log.info("config.local.json not found. Generating a new one...")

                val config = AppSettingsSchema()
                val jsonStr = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config)
                Files.writeString(CONFIG_PATH, jsonStr)

                log.info("config.local.json created successfully at: {}", CONFIG_PATH.toAbsolutePath())
                config
            } else {
                val content = Files.readString(CONFIG_PATH)
                val config = objectMapper.readValue(content, AppSettingsSchema::class.java)
                log.info("config.local.json loaded successfully!")
                config
            }
        } catch (e: Exception) {
            log.error("Fail loading configuration: {}", e.message)
            AppSettingsSchema()
        }
    }

    override fun saveConfig(config: AppSettingsSchema) {
        val validated = config.toModel()
        val json =
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(AppSettingsSchema.fromModel(validated))

        CONFIG_PATH.toAbsolutePath().parent?.let { Files.createDirectories(it) }
        Files.writeString(
            CONFIG_PATH,
            json,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        )
        cachedConfig = loadOrCreateConfig()
        log.info("Configuration saved to ${CONFIG_PATH.toAbsolutePath()}")
    }
}
