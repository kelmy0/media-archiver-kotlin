package com.kelmy.mediaarchiver.config

import com.kelmy.mediaarchiver.schemas.AppConfig

interface ConfigProvider {
    fun getConfigs(): AppConfig
}
