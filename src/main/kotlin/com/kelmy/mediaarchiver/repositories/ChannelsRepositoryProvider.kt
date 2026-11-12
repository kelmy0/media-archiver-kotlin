package com.kelmy.mediaarchiver.repositories

import com.kelmy.mediaarchiver.model.ChannelModel
import java.time.Instant

interface ChannelsRepositoryProvider {
    fun getActiveChannels(): List<ChannelModel>

    fun updateLastChecked(channels: List<ChannelModel>, timestamp: Instant)
}
