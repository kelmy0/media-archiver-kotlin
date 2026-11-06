package com.kelmy.mediaarchiver.core;

import java.time.Instant;
import java.util.List;

import com.kelmy.mediaarchiver.extractors.Extractor;
import com.kelmy.mediaarchiver.schemas.BaseChannelConfig;

public final class ChannelProcessor {
    private final List<Extractor<?, ?>> extractors;

    public ChannelProcessor(List<Extractor<?, ?>> extractors) {
        this.extractors = extractors;
    }

    public boolean processChanel(BaseChannelConfig channelConfig, String rootDownloadDir, int cooldownMinutes,
            Instant now) {
        // TODO Auto-generated method stub
        return false;
    }
}
