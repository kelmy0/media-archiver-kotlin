package com.kelmy.mediaarchiver.infrastructure.config

import com.fasterxml.jackson.annotation.JsonProperty
import com.kelmy.mediaarchiver.core.domain.model.AppSettings
import com.kelmy.mediaarchiver.core.domain.model.PathSettings
import com.kelmy.mediaarchiver.core.domain.model.SchedulerSettings
import java.nio.file.Paths

/**
 * JSON representation of [AppSettings] in `config.local.json`.
 *
 * It exists to keep the file format (`snake_case` keys, Jackson annotations) out of the domain
 * model. It is `internal` on purpose: only [JsonSettingsStore] should know it.
 * Use [toModel] / [fromModel] to convert from/to the domain model.
 */
internal data class AppSettingsSchema(
    @JsonProperty("path")
    val path: PathConfig = PathConfig(),

    @JsonProperty("cooldown_minutes")
    val cooldownMinutes: Int = 0,

    @JsonProperty("scheduler")
    val scheduler: SchedulerConfig = SchedulerConfig()
) {
    /** JSON form of [PathSettings]. */
    data class PathConfig(
        @JsonProperty("download_path")
        val downloadPath: String = Paths.get(System.getProperty("user.home"), "Videos", "MediaArchive").toString(),

        @JsonProperty("cookies_path")
        val cookiesPath: String = ""
    )

    /** JSON form of [SchedulerSettings]. */
    data class SchedulerConfig(
        @JsonProperty("enable_on_startup")
        val enableOnStartup: Boolean = false,

        @JsonProperty("interval_minutes")
        val intervalMinutes: Int = 0
    )

    /**
     * Converts this schema into the domain model.
     *
     * @throws IllegalArgumentException if the file contains values rejected by the domain rules
     *                                  (blank download path, negative minutes...).
     */
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
        /** Builds the schema that represents [model]. */
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
