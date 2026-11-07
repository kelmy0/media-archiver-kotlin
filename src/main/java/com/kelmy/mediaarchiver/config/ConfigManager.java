package com.kelmy.mediaarchiver.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kelmy.mediaarchiver.schemas.AppConfig;

import tools.jackson.databind.ObjectMapper;

public class ConfigManager implements ConfigProvider {
    private static final Path CONFIG_PATH = Paths.get("config.json");
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(ConfigManager.class);
    private AppConfig cachedConfig;

    public ConfigManager() {
        loadOrCreateConfig();
    }

    @Override
    public AppConfig getConfigs() {
        return cachedConfig;
    }

    private void loadOrCreateConfig() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                log.info("config.json not found. Generating a new one...");

                this.cachedConfig = this.createDefaultConfig();

                String jsonStr = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this.cachedConfig);
                Files.writeString(CONFIG_PATH, jsonStr);

                log.info("config.json created successfully at: {}", CONFIG_PATH.toAbsolutePath());
            } else {
                String content = Files.readString(CONFIG_PATH);
                this.cachedConfig = objectMapper.readValue(content, AppConfig.class);
                log.info("config.json loaded successfully!");
            }
        } catch (Exception e) {
            log.error("Fail loading configuration: {}", e.getMessage());
            this.cachedConfig = this.createDefaultConfig();
        }
    }

    private AppConfig createDefaultConfig() {
        AppConfig config = new AppConfig();

        String userHome = System.getProperty("user.home");
        Path defaultDownloadPath = Paths.get(userHome, "Videos", "MediaArchive");

        AppConfig.PathConfig pathConfig = new AppConfig.PathConfig();
        pathConfig.setDownloadPath(defaultDownloadPath.toString());
        pathConfig.setCookiesPath("");
        config.setPath(pathConfig);

        config.setCooldownMinutes(60);

        AppConfig.SchedulerConfig schedulerConfig = new AppConfig.SchedulerConfig();
        schedulerConfig.setEnableOnStartup(false);
        schedulerConfig.setIntervalMinutes(0);
        config.setScheduler(schedulerConfig);

        return config;
    }
}
