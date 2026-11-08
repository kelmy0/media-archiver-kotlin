package com.kelmy.mediaarchiver.core


import com.kelmy.mediaarchiver.extractors.Extractor
import com.kelmy.mediaarchiver.extractors.ExtractorProvider
import com.kelmy.mediaarchiver.schemas.BaseChannelConfig
import java.time.Instant

class ChannelProcessor(
    private val extractors: ExtractorProvider
) : ChannelProcessorProvider {

    override fun processChanel(
        channelConfig: BaseChannelConfig, rootDownloadDir: String, cooldownMinutes: Int, now: Instant
    ): Boolean {
        // TODO Auto-generated method stub
        return false
    }
}
