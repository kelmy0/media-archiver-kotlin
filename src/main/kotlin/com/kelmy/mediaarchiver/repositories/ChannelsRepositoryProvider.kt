package com.kelmy.mediaarchiver.repositories

import com.kelmy.mediaarchiver.model.ChannelModel
import com.kelmy.mediaarchiver.model.NewChannel
import java.time.Instant

interface ChannelsRepositoryProvider {
    fun getActiveChannels(): List<ChannelModel>
    fun insert(channel: NewChannel): ChannelModel
    fun existsByFeedUrl(feedUrl: String): Boolean
    fun updateLastChecked(channels: List<ChannelModel>, timestamp: Instant)
}
