package com.kelmy.mediaarchiver.config;

import java.util.List;

import com.kelmy.mediaarchiver.schemas.BaseChannelConfig;
import com.kelmy.mediaarchiver.schemas.AppConfig;

public interface ConfigProvider {
    AppConfig getConfigs();

    List<BaseChannelConfig> getChannels();

    void updateLastChecked(List<BaseChannelConfig> channels);
}
