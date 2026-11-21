package com.kelmy.mediaarchiver.bootstrap

import com.kelmy.mediaarchiver.core.application.DefaultChannelProcessor
import com.kelmy.mediaarchiver.core.application.DefaultMediaCollector
import com.kelmy.mediaarchiver.core.application.DefaultMediaDownloader
import com.kelmy.mediaarchiver.core.application.Orchestrator
import com.kelmy.mediaarchiver.core.domain.port.ChannelsRepository
import com.kelmy.mediaarchiver.core.domain.port.SettingsStore
import com.kelmy.mediaarchiver.infrastructure.config.JsonSettingsStore
import com.kelmy.mediaarchiver.infrastructure.extractor.InMemoryExtractorRegistry
import com.kelmy.mediaarchiver.infrastructure.persistence.MigrationRunner
import com.kelmy.mediaarchiver.infrastructure.persistence.SqlChannels
import com.kelmy.mediaarchiver.infrastructure.persistence.SqlDownloadHistory
import com.kelmy.mediaarchiver.infrastructure.system.DependencyChecker
import com.kelmy.mediaarchiver.infrastructure.system.ProcessCommandExecutor
import com.kelmy.mediaarchiver.ui.cli.ConsoleArchivingReporter
import com.kelmy.mediaarchiver.ui.gui.MainWindow
import javax.swing.SwingUtilities

/**
 * Application entry point and **composition root**: the only place that knows every concrete
 * class and wires them together (dependency injection by hand).
 *
 * Supported arguments:
 * - `--seed`: imports the channels of `seed.local.json` into the database and exits;
 * - `--cli`: runs one archiving routine in the terminal and exits;
 * - no arguments: opens the GUI.
 */
class Main {
    companion object {
        /**
         * Starts the application.
         *
         * Exits early (without doing anything else) when the environment is missing a required
         * tool. Pending database migrations are always applied before anything else runs.
         *
         * @param args command-line arguments (see [Main]).
         */
        @JvmStatic
        fun main(args: Array<String>) {
            if (!DependencyChecker.validateEnvironment()) {
                return
            }

            MigrationRunner.runPendingMigrations()

            val channelRepository = SqlChannels()

            if ("--seed" in args) {
                DevSeeder(channelRepository).run()
                return
            }

            val settingsStore = JsonSettingsStore()

            if ("--cli" in args) {
                runArchiving(channelRepository, settingsStore)
                return
            }

            SwingUtilities.invokeLater {
                MainWindow(
                    channelRepository,
                    settingsStore,
                    onRunArchiving = { runArchiving(channelRepository, settingsStore) }).isVisible = true
            }
        }

        /**
         * Builds the archiving object graph and runs one full routine, blocking until it ends.
         * A new graph is created on every call; nothing here keeps state between runs.
         */
        @JvmStatic
        private fun runArchiving(channelRepository: ChannelsRepository, settingsStore: SettingsStore) {
            val commandExecutor = ProcessCommandExecutor()
            val downloadHistoryRepo = SqlDownloadHistory()
            val extractorRegistry = InMemoryExtractorRegistry.createDefault()
            val mediaCollector = DefaultMediaCollector(downloadHistoryRepo)
            val mediaDownloader = DefaultMediaDownloader(downloadHistoryRepo, commandExecutor)
            val channelProcessor =
                DefaultChannelProcessor(extractorRegistry, mediaCollector, mediaDownloader, commandExecutor)
            val orchestrator =
                Orchestrator(settingsStore, channelProcessor, channelRepository, ConsoleArchivingReporter())

            orchestrator.run()
        }
    }
}
