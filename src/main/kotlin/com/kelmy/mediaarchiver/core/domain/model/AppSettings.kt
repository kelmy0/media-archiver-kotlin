package com.kelmy.mediaarchiver.core.domain.model

import java.nio.file.Paths

data class AppSettings(
    val path: PathSettings = PathSettings(),
    val cooldownMinutes: Int = 0,
    val scheduler: SchedulerSettings = SchedulerSettings()
) {
    init {
        AppSettingsValidator.validate(this)
    }
}

data class PathSettings(
    val downloadPath: String = Paths.get(System.getProperty("user.home"), "Videos", "MediaArchive").toString(),
    val cookiesPath: String = ""
)

data class SchedulerSettings(
    val enableOnStartup: Boolean = false,
    val intervalMinutes: Int = 0
)

object AppSettingsValidator {
    @JvmStatic
    fun validate(settings: AppSettings) {
        require(settings.path.downloadPath.isNotBlank()) { "downloadPath cant be blank" }
        require(settings.cooldownMinutes >= 0) { "cooldownMinutes must be >= 0" }
        require(settings.scheduler.intervalMinutes >= 0) { "intervalMinutes must be >= 0" }
    }
}
