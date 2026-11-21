package com.kelmy.mediaarchiver.bootstrap

import com.kelmy.mediaarchiver.infrastructure.config.SettingsManager
import com.kelmy.mediaarchiver.core.domain.port.SettingsStore
import com.kelmy.mediaarchiver.infrastructure.system.DependencyChecker
import com.kelmy.mediaarchiver.core.application.DefaultChannelProcessor
import com.kelmy.mediaarchiver.core.application.DefaultMediaCollector
import com.kelmy.mediaarchiver.core.application.DefaultMediaDownloader
import com.kelmy.mediaarchiver.core.application.Orchestrator
import com.kelmy.mediaarchiver.infrastructure.persistence.MigrationRunner
import com.kelmy.mediaarchiver.infrastructure.extractor.ExtractorRegistry
import com.kelmy.mediaarchiver.infrastructure.persistence.SqlChannels
import com.kelmy.mediaarchiver.core.domain.port.ChannelsRepository
import com.kelmy.mediaarchiver.infrastructure.persistence.SqlDownloadHistory
import com.kelmy.mediaarchiver.ui.gui.MainWindow
import com.kelmy.mediaarchiver.ui.cli.AnsiColors
import javax.swing.SwingUtilities

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            if (!DependencyChecker.validateEnviroment()) {
                return
            }

            MigrationRunner.runPendingMigrations()

            val channelRepository = SqlChannels()

            if ("--seed" in args) {
                DevSeeder(channelRepository).run()
                return
            }


            val configManager = SettingsManager()

            if ("--cli" in args) {
                runArchiving(channelRepository, configManager)
                return
            }

            SwingUtilities.invokeLater {
                MainWindow(
                    channelRepository,
                    configManager,
                    onRunArchiving = { runArchiving(channelRepository, configManager) }).isVisible = true
            }
        }

        @JvmStatic
        private fun runArchiving(channelRepository: ChannelsRepository, configProvider: SettingsStore) {
            val downloadHistoryRepo = SqlDownloadHistory()
            val extractorRegistry = ExtractorRegistry.createDefault()
            val mediaCollector = DefaultMediaCollector(downloadHistoryRepo)
            val mediaDownloader = DefaultMediaDownloader(downloadHistoryRepo)
            val channelProcessor = DefaultChannelProcessor(extractorRegistry, mediaCollector, mediaDownloader)
            val orchestrator = Orchestrator(configProvider, channelProcessor, channelRepository)

            println("${AnsiColors.CYAN}==============================${AnsiColors.RESET}")
            println("${AnsiColors.BOLD}STARTING ARCHIVING ROUTINE${AnsiColors.RESET}")
            println("${AnsiColors.CYAN}==============================${AnsiColors.RESET}")

            orchestrator.run()

            println("${AnsiColors.CYAN}==============================${AnsiColors.RESET}")
            println("${AnsiColors.BOLD}ARCHIVING ROUTINE COMPLETED${AnsiColors.RESET}")
            println("${AnsiColors.CYAN}==============================${AnsiColors.RESET}")
        }
    }
}