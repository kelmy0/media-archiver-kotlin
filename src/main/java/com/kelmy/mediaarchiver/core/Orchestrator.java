package com.kelmy.mediaarchiver.core;

import java.time.Instant;
import java.util.List;

import com.kelmy.mediaarchiver.config.ConfigProvider;
import com.kelmy.mediaarchiver.config.DependencyChecker;
import com.kelmy.mediaarchiver.database.MigrationRunner;
import com.kelmy.mediaarchiver.schemas.AppConfig;
import com.kelmy.mediaarchiver.schemas.BaseChannelConfig;
import com.kelmy.mediaarchiver.utils.AnsiColors;

public final class Orchestrator {
    private final ConfigProvider configProvider;
    private final ChannelProcessor channelProcessor;
    private final ChannelsRepositoryProvider channelProvider;

    public Orchestrator(ConfigProvider configProvider, ChannelProcessor channelProcessor,
            ChannelsRepositoryProvider channelProvider) {
        this.configProvider = configProvider;
        this.channelProcessor = channelProcessor;
        this.channelProvider = channelProvider;
    }

    public void run() {
        if (!DependencyChecker.validateEnviroment()) {
            return;
        }

        MigrationRunner.runPendingMigrations();

        AppConfig configs = this.configProvider.getConfigs();

        List<BaseChannelConfig> channels = this.channelProvider.getActiveChannels();

        if (channels.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        boolean thereIsChanges = false;
        int iterator = 1;

        for (BaseChannelConfig channel : channels) {
            String name = channel.getName();
            String platform = channel.getPlatform();

            System.out.println(AnsiColors.CYAN + "--------------------------------------" + AnsiColors.RESET);
            System.out.printf(AnsiColors.BOLD + "[%d/%d]  Processing: %s (Platform: %s)%n" + AnsiColors.RESET,
                    iterator++, channels.size(), name, platform);
            System.out.println(AnsiColors.CYAN + "--------------------------------------" + AnsiColors.RESET);

            boolean processedSuccessfully = this.channelProcessor.processChanel(channel, platform,
                    configs.getCooldownMinutes(), now);

            if (processedSuccessfully) {
                thereIsChanges = true;
            }
        }

        if (thereIsChanges) {
            this.channelProvider.updateLastChecked(channels, now);
        }
    }
}
