package com.kelmy.mediaarchiver.core

import com.kelmy.mediaarchiver.config.ConfigProvider
import com.kelmy.mediaarchiver.config.DependencyChecker
import com.kelmy.mediaarchiver.database.MigrationRunner
import com.kelmy.mediaarchiver.repositories.ChannelsRepositoryProvider
import com.kelmy.mediaarchiver.schemas.AppConfig
import com.kelmy.mediaarchiver.schemas.BaseChannelConfig
import com.kelmy.mediaarchiver.utils.AnsiColors
import java.time.Instant

class Orchestrator(
    private val configProvider: ConfigProvider,
    private val channelProcessorProvider: ChannelProcessorProvider,
    private val channelsRepositoryProvider: ChannelsRepositoryProvider
) {

    fun run() {
        if (!DependencyChecker.validateEnviroment()) {
            return
        }

        MigrationRunner.runPendingMigrations()

        val configs: AppConfig = this.configProvider.getConfigs()

        val channels: List<BaseChannelConfig> = this.channelsRepositoryProvider.getActiveChannels()

        if (channels.isEmpty()) {
            return
        }

        val downloadPath = configs.path.downloadPath
        val cooldown = configs.cooldownMinutes
        val now: Instant = Instant.now()
        var thereIsChanges = false

        for ((index, channel) in channels.withIndex()) {
            val current = index + 1
            val name = channel.name
            val platform = channel.platform

            println("${AnsiColors.CYAN}------------------------------------------------${AnsiColors.RESET}")
            println("${AnsiColors.BOLD}[$current/${channels.size}]  Processing: $name (Platform: $platform)${AnsiColors.RESET}")
            println("${AnsiColors.CYAN}------------------------------------------------${AnsiColors.RESET}")

            val processedSuccessfully =
                this.channelProcessorProvider.processChanel(channel, downloadPath, cooldown, now)

            if (processedSuccessfully) {
                thereIsChanges = true
            }
        }

        if (thereIsChanges) {
            this.channelsRepositoryProvider.updateLastChecked(channels, now)
        }
    }
}