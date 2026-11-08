package com.kelmy.mediaarchiver

import com.kelmy.mediaarchiver.config.ConfigManager
import com.kelmy.mediaarchiver.config.ConfigProvider
import com.kelmy.mediaarchiver.core.ChannelProcessor
import com.kelmy.mediaarchiver.repositories.ChannelsRepositoryProvider
import com.kelmy.mediaarchiver.core.Orchestrator
import com.kelmy.mediaarchiver.extractors.Extractor
import com.kelmy.mediaarchiver.extractors.ExtractorRegistry
import com.kelmy.mediaarchiver.repositories.ChannelsRepository
import com.kelmy.mediaarchiver.utils.AnsiColors

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val extractorRegistry = ExtractorRegistry.createDefault()
            val configManager = ConfigManager()
            val channelProcessor = ChannelProcessor(extractorRegistry)
            val channelRepository = ChannelsRepository()

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