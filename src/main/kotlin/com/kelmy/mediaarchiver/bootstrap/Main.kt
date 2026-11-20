package com.kelmy.mediaarchiver.bootstrap

import com.kelmy.mediaarchiver.config.ConfigManager
import com.kelmy.mediaarchiver.config.DependencyChecker
import com.kelmy.mediaarchiver.core.ChannelProcessor
import com.kelmy.mediaarchiver.core.MediaCollector
import com.kelmy.mediaarchiver.core.MediaDownloader
import com.kelmy.mediaarchiver.core.Orchestrator
import com.kelmy.mediaarchiver.database.MigrationRunner
import com.kelmy.mediaarchiver.extractors.ExtractorRegistry
import com.kelmy.mediaarchiver.repositories.ChannelsRepository
import com.kelmy.mediaarchiver.repositories.ChannelsRepositoryProvider
import com.kelmy.mediaarchiver.repositories.DownloadHistoryRepository
import com.kelmy.mediaarchiver.utils.AnsiColors

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            if (!DependencyChecker.validateEnviroment()) {
                return
            }

            MigrationRunner.runPendingMigrations()

            val channelRepository = ChannelsRepository()

            if ("--seed" in args) {
                DevSeeder(channelRepository).run()
                return
            }

            runArchiving(channelRepository)
        }

        private fun runArchiving(channelRepository: ChannelsRepositoryProvider) {
            val downloadHistoryRepo = DownloadHistoryRepository()
            val extractorRegistry = ExtractorRegistry.createDefault()
            val configManager = ConfigManager()
            val mediaCollector = MediaCollector(downloadHistoryRepo)
            val mediaDownloader = MediaDownloader(downloadHistoryRepo)
            val channelProcessor = ChannelProcessor(extractorRegistry, mediaCollector, mediaDownloader)
            val orchestrator = Orchestrator(configManager, channelProcessor, channelRepository)

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