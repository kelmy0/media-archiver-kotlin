package com.kelmy.mediaarchiver.core;

import java.time.Instant;
import java.util.List;

import com.kelmy.mediaarchiver.schemas.BaseChannelConfig;

public interface ChannelsRepositoryProvider {
    List<BaseChannelConfig> getActiveChannels();

    void updateLastChecked(List<BaseChannelConfig> channels, Instant timestamp);
}
