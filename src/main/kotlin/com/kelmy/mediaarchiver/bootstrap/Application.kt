package com.kelmy.mediaarchiver.bootstrap

import com.kelmy.mediaarchiver.archiving.application.ArchiveFileFallback
import com.kelmy.mediaarchiver.archiving.application.DefaultChannelProcessor
import com.kelmy.mediaarchiver.archiving.application.DefaultMediaCollector
import com.kelmy.mediaarchiver.archiving.application.DefaultMediaDownloader
import com.kelmy.mediaarchiver.archiving.application.Orchestrator
import com.kelmy.mediaarchiver.archiving.domain.port.ExtractorRegistry
import com.kelmy.mediaarchiver.archiving.infrastructure.extractor.ExtractorVideoIdResolver
import com.kelmy.mediaarchiver.archiving.infrastructure.extractor.InMemoryExtractorRegistry
import com.kelmy.mediaarchiver.archiving.infrastructure.filesystem.FileSystemChannelFolderReader
import com.kelmy.mediaarchiver.archiving.infrastructure.persistence.SqlImportedFolders
import com.kelmy.mediaarchiver.archiving.infrastructure.system.DependencyChecker
import com.kelmy.mediaarchiver.archiving.infrastructure.system.ProcessCommandExecutor
import com.kelmy.mediaarchiver.backup.application.BackupService
import com.kelmy.mediaarchiver.backup.infrastructure.JsonBackupStore
import com.kelmy.mediaarchiver.library.application.ChannelService
import com.kelmy.mediaarchiver.library.application.VideoService
import com.kelmy.mediaarchiver.library.domain.port.ChannelsRepository
import com.kelmy.mediaarchiver.library.domain.port.VideoRepository
import com.kelmy.mediaarchiver.library.infrastructure.persistence.SqlChannels
import com.kelmy.mediaarchiver.library.infrastructure.persistence.SqlVideos
import com.kelmy.mediaarchiver.presentation.AppServices
import com.kelmy.mediaarchiver.presentation.UiFactory
import com.kelmy.mediaarchiver.presentation.UiFactoryProvider
import com.kelmy.mediaarchiver.settings.domain.port.SettingsStore
import com.kelmy.mediaarchiver.settings.infrastructure.JsonSettingsStore
import com.kelmy.mediaarchiver.shared.infrastructure.persistence.MigrationRunner

/**
 * **Composition root**: the only place that knows every concrete class and wires them together
 * (dependency injection by hand). Static class: it is an `object` and keeps no state.
 */
internal object Application {
    /**
     * Starts the application.
     *
     * Exits early (without doing anything else) when the environment is missing a required tool.
     * Pending database migrations are always applied before anything else runs.
     *
     * @param args command-line arguments: `--seed` imports `seed.local.json` and exits; `--cli` or `--gui`
     *             choose the interface (see [UiFactoryProvider]).
     */
    fun run(args: Array<String>) {
        if (!DependencyChecker.validateEnvironment()) {
            return
        }

        MigrationRunner.runPendingMigrations()

        val channelsRepository = SqlChannels()

        if ("--seed" in args) {
            DevSeeder(channelsRepository).run()
            return
        }

        // The abstract factory decides which interface family is created; nothing below depends on it.
        val uiFactory = UiFactoryProvider.create(UiFactoryProvider.modeFrom(args))
        val settingsStore = JsonSettingsStore()
        val videosRepository = SqlVideos()
        val extractors = InMemoryExtractorRegistry.createDefault()

        val channelService = ChannelService(channelsRepository, videosRepository)
        val services = AppServices(
            channels = channelService,
            videos = VideoService(videosRepository, channelsRepository, ExtractorVideoIdResolver(extractors)),
            backup = BackupService(channelService, videosRepository, JsonBackupStore()),
            settings = settingsStore,
            platforms = extractors.all().map { it.name }.sorted(),
            runArchiving = { runArchiving(channelsRepository, videosRepository, settingsStore, extractors, uiFactory) }
        )

        uiFactory.createApplicationView(services).show()
    }

    /**
     * Builds the archiving object graph and runs one full routine, blocking until it ends.
     * A new graph is created on every call; nothing here keeps state between runs.
     */
    private fun runArchiving(
        channelsRepository: ChannelsRepository,
        videosRepository: VideoRepository,
        settingsStore: SettingsStore,
        extractors: ExtractorRegistry,
        uiFactory: UiFactory
    ) {
        val commandExecutor = ProcessCommandExecutor()
        val mediaCollector = DefaultMediaCollector(videosRepository)
        val mediaDownloader = DefaultMediaDownloader(videosRepository, commandExecutor)
        val archiveFileFallback =
            ArchiveFileFallback(FileSystemChannelFolderReader(), SqlImportedFolders(), videosRepository)
        val channelProcessor =
            DefaultChannelProcessor(extractors, mediaCollector, mediaDownloader, archiveFileFallback, commandExecutor)

        Orchestrator(settingsStore, channelProcessor, channelsRepository, uiFactory.createProgressListener()).run()
    }
}
