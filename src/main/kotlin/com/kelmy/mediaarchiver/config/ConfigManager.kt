package com.kelmy.mediaarchiver.config

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.kelmy.mediaarchiver.schemas.AppConfig

import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper

class ConfigManager : ConfigProvider {
    companion object {
        private val CONFIG_PATH: Path = Paths.get("config.json")
        private val objectMapper = jacksonObjectMapper()
        private val log = LoggerFactory.getLogger(ConfigManager::class.java)
    }

    private val cachedConfig: AppConfig = loadOrCreateConfig()

    override fun getConfigs(): AppConfig = cachedConfig

    private fun loadOrCreateConfig(): AppConfig {
        return try {
            if (!Files.exists(CONFIG_PATH)) {
                log.info("config.json not found. Generating a new one...")

                val config = AppConfig()
                val jsonStr = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config)
                Files.writeString(CONFIG_PATH, jsonStr)

                log.info("config.json created successfully at: {}", CONFIG_PATH.toAbsolutePath())
                config
            } else {
                val content = Files.readString(CONFIG_PATH)
                val config = objectMapper.readValue(content, AppConfig::class.java)
                log.info("config.json loaded successfully!")
                config
            }
        } catch (e: Exception) {
            log.error("Fail loading configuration: {}", e.message)
            AppConfig()
        }
    }
}
