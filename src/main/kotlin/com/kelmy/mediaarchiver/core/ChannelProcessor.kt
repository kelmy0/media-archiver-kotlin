package com.kelmy.mediaarchiver.core

import java.time.Instant

import com.kelmy.mediaarchiver.extractors.Extractor
import com.kelmy.mediaarchiver.schemas.BaseChannelConfig

class ChannelProcessor(
    private val extractors: List<Extractor<*>>
) {

    fun processChanel(
        channelConfig: BaseChannelConfig, rootDownloadDir: String, cooldownMinutes: Int, now: Instant
    ): Boolean {
        // TODO Auto-generated method stub
        return false
    }
}
