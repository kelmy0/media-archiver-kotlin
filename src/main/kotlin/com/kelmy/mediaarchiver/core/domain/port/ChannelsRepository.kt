package com.kelmy.mediaarchiver.core.domain.port

import com.kelmy.mediaarchiver.core.domain.model.Channel
import com.kelmy.mediaarchiver.core.domain.model.NewChannel
import java.time.Instant

interface ChannelsRepository {
    fun insert(channel: NewChannel): Channel
    fun getChannels(): List<Channel>
    fun getActiveChannels(): List<Channel>
    fun updateChannel(channel: Channel): Boolean
    fun existsByFeedUrl(feedUrl: String): Boolean
    fun updateLastChecked(channels: List<Channel>, timestamp: Instant)
    fun deleteChannel(id: Int): Boolean
}
