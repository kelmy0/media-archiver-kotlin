package com.kelmy.mediaarchiver.schemas;

import java.time.Instant;
import java.util.Objects;

/*
* Common fields to any channel on any platform
*/
public class BaseChannelConfig {
        private final Integer id;
        private String name;
        private String platform;
        private String feedUrl;
        private String folderName;
        private Boolean active;
        private Instant lastChecked;
        private String optionsJson;

        public BaseChannelConfig(Integer id, String name, String platform, String feedUrl, String folderName,
                        Boolean active, Instant lastChecked, String optionsJson) {
                this.id = Objects.requireNonNull(id, "id cant be null");
                this.name = Objects.requireNonNull(name, "name cant be null");
                this.platform = Objects.requireNonNull(platform, "platform cant be null");
                this.feedUrl = Objects.requireNonNull(feedUrl, "feedUrl cant be null");
                this.folderName = Objects.requireNonNull(folderName, "folderName cant be null");
                this.active = Objects.requireNonNull(active, "active cant be null");
                this.lastChecked = lastChecked != null ? lastChecked : Instant.EPOCH;
                this.optionsJson = optionsJson != null ? optionsJson : "{}";
        }

        public Integer getId() {
                return id;
        }

        public String getName() {
                return name;
        }

        public void setName(String name) {
                this.name = name;
        }

        public String getPlatform() {
                return platform;
        }

        public void setPlatform(String platform) {
                this.platform = platform;
        }

        public String getFeedUrl() {
                return feedUrl;
        }

        public void setFeedUrl(String feedUrl) {
                this.feedUrl = feedUrl;
        }

        public String getFolderName() {
                return folderName;
        }

        public void setFolderName(String folderName) {
                this.folderName = folderName;
        }

        public Boolean getActive() {
                return this.active;
        }

        public void setActive(boolean active) {
                this.active = active;
        }

        public Instant getLastChecked() {
                return lastChecked;
        }

        public void setLastChecked(Instant lastChecked) {
                this.lastChecked = lastChecked;
        }

        public String getOptionsJson() {
                return this.optionsJson;
        }

        public void setOptionsJson(String optionsJson) {
                this.optionsJson = optionsJson;
        }
}