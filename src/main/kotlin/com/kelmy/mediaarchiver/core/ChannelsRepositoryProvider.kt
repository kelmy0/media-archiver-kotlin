package com.kelmy.mediaarchiver.core

import java.time.Instant
import com.kelmy.mediaarchiver.schemas.BaseChannelConfig

interface ChannelsRepositoryProvider {
    fun getActiveChannels(): List<BaseChannelConfig>

    fun updateLastChecked(channels: List<BaseChannelConfig>, timestamp: Instant)
}
