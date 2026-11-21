package com.kelmy.mediaarchiver.core.domain.port

import com.kelmy.mediaarchiver.core.domain.model.AppSettings


interface SettingsStore {
    fun getConfigs(): AppSettings
    fun saveConfig(config: AppSettings)
}
