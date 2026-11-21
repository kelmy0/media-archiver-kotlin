package com.kelmy.mediaarchiver.core.application

import com.kelmy.mediaarchiver.core.domain.model.Channel
import java.time.Instant

interface ChannelProcessor {
    fun processChannel(
        channel: Channel, rootDownloadDir: String, cooldownMinutes: Int, now: Instant
    ): Boolean
}