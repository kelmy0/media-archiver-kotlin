package com.kelmy.mediaarchiver.repositories

import com.kelmy.mediaarchiver.model.ChannelModel
import com.kelmy.mediaarchiver.model.NewChannel
import java.time.Instant

interface ChannelsRepositoryProvider {
    fun insert(channel: NewChannel): ChannelModel
    fun getChannels(): List<ChannelModel>
    fun getActiveChannels(): List<ChannelModel>
    fun updateChannel(channel: ChannelModel): Boolean
    fun existsByFeedUrl(feedUrl: String): Boolean
    fun updateLastChecked(channels: List<ChannelModel>, timestamp: Instant)
    fun deleteChannel(id: Int): Boolean
}
