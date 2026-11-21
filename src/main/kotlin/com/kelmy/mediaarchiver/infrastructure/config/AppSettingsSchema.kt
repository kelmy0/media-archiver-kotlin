package com.kelmy.mediaarchiver.infrastructure.config

import com.fasterxml.jackson.annotation.JsonProperty
import com.kelmy.mediaarchiver.core.domain.model.AppSettings
import com.kelmy.mediaarchiver.core.domain.model.PathSettings
import com.kelmy.mediaarchiver.core.domain.model.SchedulerSettings
import java.nio.file.Paths

internal data class AppSettingsSchema(
    @JsonProperty("path")
    val path: PathConfig = PathConfig(),

    @JsonProperty("cooldown_minutes")
    val cooldownMinutes: Int = 0,

    @JsonProperty("scheduler")
    val scheduler: SchedulerConfig = SchedulerConfig()
) {
    data class PathConfig(
        @JsonProperty("download_path")
        val downloadPath: String = Paths.get(System.getProperty("user.home"), "Videos", "MediaArchive").toString(),

        @JsonProperty("cookies_path")
        val cookiesPath: String = ""
    )

    data class SchedulerConfig(
        @JsonProperty("enable_on_startup")
        val enableOnStartup: Boolean = false,

        @JsonProperty("interval_minutes")
        val intervalMinutes: Int = 0
    )

    fun toModel(): AppSettings = AppSettings(
        path = PathSettings(
            downloadPath = path.downloadPath,
            cookiesPath = path.cookiesPath
        ),
        cooldownMinutes = cooldownMinutes,
        scheduler = SchedulerSettings(
            enableOnStartup = scheduler.enableOnStartup,
            intervalMinutes = scheduler.intervalMinutes
        )
    )

    companion object {
        fun fromModel(model: AppSettings): AppSettingsSchema = AppSettingsSchema(
            path = PathConfig(
                downloadPath = model.path.downloadPath,
                cookiesPath = model.path.cookiesPath
            ),
            cooldownMinutes = model.cooldownMinutes,
            scheduler = SchedulerConfig(
                enableOnStartup = model.scheduler.enableOnStartup,
                intervalMinutes = model.scheduler.intervalMinutes
            )
        )
    }
}
