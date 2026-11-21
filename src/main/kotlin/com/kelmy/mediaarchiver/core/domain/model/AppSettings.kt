package com.kelmy.mediaarchiver.core.domain.model

import java.nio.file.Paths

/**
 * Application-wide settings, independent of how they are persisted.
 *
 * The constructor validates its own invariants through [AppSettingsValidator], so an
 * instance of this class is always in a valid state.
 *
 * @property path            Filesystem locations used by the application.
 * @property cooldownMinutes Minimum number of minutes that must pass before a channel
 *                           is scanned again. `0` disables the cooldown.
 * @property scheduler       Settings for the (future) automatic execution.
 * @throws IllegalArgumentException if any invariant checked by [AppSettingsValidator] is violated.
 */
data class AppSettings(
    val path: PathSettings = PathSettings(),
    val cooldownMinutes: Int = 0,
    val scheduler: SchedulerSettings = SchedulerSettings()
) {
    init {
        AppSettingsValidator.validate(this)
    }
}

/**
 * Filesystem locations used by the application.
 *
 * @property downloadPath Root directory where each channel gets its own sub-folder.
 *                        Defaults to `<user.home>/Videos/MediaArchive`.
 * @property cookiesPath  Optional path to a `cookies.txt` file. Blank means "not configured".
 */
data class PathSettings(
    val downloadPath: String = Paths.get(System.getProperty("user.home"), "Videos", "MediaArchive").toString(),
    val cookiesPath: String = ""
)

/**
 * Settings for automatic (scheduled) execution of the archiving routine.
 *
 * @property enableOnStartup Whether the routine should start automatically when the app opens.
 * @property intervalMinutes Interval between automatic runs, in minutes. `0` means "no interval".
 */
data class SchedulerSettings(
    val enableOnStartup: Boolean = false,
    val intervalMinutes: Int = 0
)

/**
 * Business rules that every [AppSettings] instance must satisfy.
 */
object AppSettingsValidator {
    /**
     * Validates [settings].
     *
     * @param settings the settings to be checked.
     * @throws IllegalArgumentException if the download path is blank or if the cooldown
     *                                  or the scheduler interval is negative.
     */
    @JvmStatic
    fun validate(settings: AppSettings) {
        require(settings.path.downloadPath.isNotBlank()) { "downloadPath cant be blank" }
        require(settings.cooldownMinutes >= 0) { "cooldownMinutes must be >= 0" }
        require(settings.scheduler.intervalMinutes >= 0) { "intervalMinutes must be >= 0" }
    }
}
