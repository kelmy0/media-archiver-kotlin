package com.kelmy.mediaarchiver.config;

import java.util.List;

import com.kelmy.mediaarchiver.schemas.AppConfig;
import com.kelmy.mediaarchiver.schemas.BaseChannelConfig;

public class ConfigManager implements ConfigProvider {
    @Override
    public List<BaseChannelConfig> getChannels() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public AppConfig getConfigs() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateLastChecked(List<BaseChannelConfig> channels) {
        // TODO Auto-generated method stub

    }

}
