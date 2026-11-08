package com.kelmy.mediaarchiver.repositories

import java.time.Instant
import com.kelmy.mediaarchiver.schemas.BaseChannelConfig

interface ChannelsRepositoryProvider {
    fun getActiveChannels(): List<BaseChannelConfig>

    fun updateLastChecked(channels: List<BaseChannelConfig>, timestamp: Instant)
}
