package com.kelmy.mediaarchiver.presentation

import com.kelmy.mediaarchiver.backup.application.BackupService
import com.kelmy.mediaarchiver.library.application.ChannelService
import com.kelmy.mediaarchiver.library.application.VideoService
import com.kelmy.mediaarchiver.settings.domain.port.SettingsStore

/**
 * Everything a user interface needs from the rest of the application, bundled in one object.
 *
 * Both interfaces (textual and graphical) receive the same instance, so neither of them knows how
 * the services were built.
 *
 * @property channels      channel CRUD.
 * @property videos        video CRUD (including associating a video to a channel).
 * @property backup        save all data to a file / load data from a file.
 * @property settings      application settings.
 * @property platforms     names of the platforms that can be chosen for a channel (one per extractor).
 * @property runArchiving  runs one full archiving routine; it blocks until the routine ends, so
 *                         interfaces that must stay responsive call it from a background thread.
 */
class AppServices(
    val channels: ChannelService,
    val videos: VideoService,
    val backup: BackupService,
    val settings: SettingsStore,
    val platforms: List<String>,
    val runArchiving: () -> Unit
)
