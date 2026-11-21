package com.kelmy.mediaarchiver.config

import com.kelmy.mediaarchiver.schemas.AppSettingsSchema

interface ConfigProvider {
    fun getConfigs(): AppSettingsSchema
    fun saveConfig(config: AppSettingsSchema)
}
