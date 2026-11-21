package com.kelmy.mediaarchiver.core.application

import com.kelmy.mediaarchiver.core.domain.port.SettingsStore
import com.kelmy.mediaarchiver.core.domain.port.ChannelsRepository
import com.kelmy.mediaarchiver.ui.cli.AnsiColors
import java.time.Instant

class Orchestrator(
    private val configProvider: SettingsStore,
    private val channelProcessorProvider: ChannelProcessor,
    private val channelsRepositoryProvider: ChannelsRepository
) {

    fun run() {
        val configs = this.configProvider.getConfigs()

        val channels = this.channelsRepositoryProvider.getActiveChannels()

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
                this.channelProcessorProvider.processChannel(channel, downloadPath, cooldown, now)

            if (processedSuccessfully) {
                thereIsChanges = true
            }
        }

        if (thereIsChanges) {
            this.channelsRepositoryProvider.updateLastChecked(channels, now)
        }
    }
}