package com.kelmy.mediaarchiver.model

import java.nio.file.Paths

data class AppSettingsModel(
    val path: PathSettingsModel = PathSettingsModel(),
    val cooldownMinutes: Int = 0,
    val scheduler: SchedulerSettingsModel = SchedulerSettingsModel()
) {
    init {
        AppSettingsValidator.validate(this)
    }
}

data class PathSettingsModel(
    val downloadPath: String = Paths.get(System.getProperty("user.home"), "Videos", "MediaArchive").toString(),
    val cookiesPath: String = ""
)

data class SchedulerSettingsModel(
    val enableOnStartup: Boolean = false,
    val intervalMinutes: Int = 0
)

object AppSettingsValidator {
    @JvmStatic
    fun validate(settings: AppSettingsModel) {
        require(settings.path.downloadPath.isNotBlank()) { "downloadPath cant be blank" }
        require(settings.cooldownMinutes >= 0) { "cooldownMinutes must be >= 0" }
        require(settings.scheduler.intervalMinutes >= 0) { "intervalMinutes must be >= 0" }
    }
}
