package com.kelmy.mediaarchiver.core


import com.kelmy.mediaarchiver.extractors.ExtractorProvider
import com.kelmy.mediaarchiver.model.ChannelModel
import java.time.Instant

class ChannelProcessor(
    private val extractors: ExtractorProvider
) : ChannelProcessorProvider {

    override fun processChanel(
        channelConfig: ChannelModel, rootDownloadDir: String, cooldownMinutes: Int, now: Instant
    ): Boolean {
        // TODO Auto-generated method stub
        return false
    }
}
