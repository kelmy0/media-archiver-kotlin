package com.kelmy.mediaarchiver.core

import com.kelmy.mediaarchiver.schemas.BaseChannelConfig
import java.time.Instant

interface ChannelProcessorProvider {
    fun processChanel(
        channelConfig: BaseChannelConfig, rootDownloadDir: String, cooldownMinutes: Int, now: Instant
    ): Boolean
}