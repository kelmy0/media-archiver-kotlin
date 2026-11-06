package com.kelmy.mediaarchiver.schemas;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AppConfig {
    @JsonProperty("path")
    private PathConfig path;

    @JsonProperty("cooldown_minutes")
    private int cooldownMinutes;

    @JsonProperty("scheduler")
    private SchedulerConfig scheduler;

    public PathConfig getPath() {
        return path;
    }

    public void setPath(PathConfig path) {
        this.path = path;
    }

    public int getCooldownMinutes() {
        return cooldownMinutes;
    }

    public void setCooldownMinutes(int cooldownMinutes) {
        this.cooldownMinutes = cooldownMinutes;
    }

    public SchedulerConfig getScheduler() {
        return scheduler;
    }

    public void setScheduler(SchedulerConfig scheduler) {
        this.scheduler = scheduler;
    }

    public static class PathConfig {
        @JsonProperty("download_path")
        private String downloadPath;

        @JsonProperty("cookies_path")
        private String cookiesPath;

        public String getDownloadPath() {
            return downloadPath;
        }

        public void setDownloadPath(String downloadPath) {
            this.downloadPath = downloadPath;
        }

        public String getCookiesPath() {
            return cookiesPath;
        }

        public void setCookiesPath(String cookiesPath) {
            this.cookiesPath = cookiesPath;
        }

        public Path getResolvedDownloadPath() {
            String resolved = downloadPath;
            if (resolved != null && resolved.contains("%USERPROFILE%")) {
                String userProfile = System.getenv("USERPROFILE");

                if (userProfile != null) {
                    resolved = resolved.replace("%USERPROFILE%", userProfile);
                }
            }
            return Paths.get(resolved);
        }

        public Path getResolvedCookiesPath() {
            if (cookiesPath == null || cookiesPath.isBlank()) {
                return null;
            }
            return Paths.get(cookiesPath);
        }
    }

    public static class SchedulerConfig {
        @JsonProperty("enable_on_startup")
        private boolean enableOnStartup;

        @JsonProperty("interval_minutes")
        private int intervalMinutes;

        public boolean isEnableOnStartup() {
            return enableOnStartup;
        }

        public void setEnableOnStartup(boolean enableOnStartup) {
            this.enableOnStartup = enableOnStartup;
        }

        public int getIntervalMinutes() {
            return intervalMinutes;
        }

        public void setIntervalMinutes(int intervalMinutes) {
            this.intervalMinutes = intervalMinutes;
        }
    }
}
