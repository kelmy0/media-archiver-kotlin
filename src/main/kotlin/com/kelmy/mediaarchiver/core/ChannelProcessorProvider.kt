package com.kelmy.mediaarchiver.core

import com.kelmy.mediaarchiver.model.ChannelModel
import java.time.Instant

interface ChannelProcessorProvider {
    fun processChanel(
        channelConfig: ChannelModel, rootDownloadDir: String, cooldownMinutes: Int, now: Instant
    ): Boolean
}