package com.kelmy.mediaarchiver.schemas

import com.fasterxml.jackson.annotation.JsonProperty
import java.nio.file.Paths

data class AppConfig(
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
}
