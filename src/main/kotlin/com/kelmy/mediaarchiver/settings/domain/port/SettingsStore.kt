package com.kelmy.mediaarchiver.settings.domain.port

import com.kelmy.mediaarchiver.settings.domain.model.AppSettings

/**
 * Port for reading and persisting the application settings.
 *
 * Implementations decide where and how the settings are stored (JSON file, database...);
 * the rest of the application only sees [AppSettings].
 */
interface SettingsStore {
    /** Returns the current settings. Never `null`: implementations fall back to defaults. */
    fun current(): AppSettings

    /**
     * Persists [settings] and makes them the new [current] value.
     */
    fun save(settings: AppSettings)
}
