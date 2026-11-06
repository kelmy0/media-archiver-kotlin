package com.kelmy.mediaarchiver.schemas;

public class YoutubeChannelConfig extends BaseChannelConfig {
    private boolean fetchVideos = true;
    private boolean fetchShorts = false;

    public boolean isFetchVideos() {
        return fetchVideos;
    }

    public void setFetchVideos(boolean fetchVideos) {
        this.fetchVideos = fetchVideos;
    }

    public boolean isFetchShorts() {
        return fetchShorts;
    }

    public void setFetchShorts(boolean fetchShorts) {
        this.fetchShorts = fetchShorts;
    }
}
