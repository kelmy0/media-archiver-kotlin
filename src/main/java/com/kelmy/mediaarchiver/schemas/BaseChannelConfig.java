package com.kelmy.mediaarchiver.schemas;

import java.time.Instant;

/*
* Common fields to any channel on any platform
*/
public class BaseChannelConfig {
        private String name;
        private String platform;
        private String feedUrl;
        private String folderName;
        private Instant lastChecked;

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

        public Instant getLastChecked() {
                return lastChecked;
        }

        public void setLastChecked(Instant lastChecked) {
                this.lastChecked = lastChecked;
        }
}