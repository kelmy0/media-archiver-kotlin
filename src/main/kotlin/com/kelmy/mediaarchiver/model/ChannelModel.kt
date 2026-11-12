package com.kelmy.mediaarchiver.model

import java.time.Instant

data class ChannelModel(
    val id: Int,
    val name: String,
    val platform: String,
    val feedUrl: String,
    val folderName: String,
    val active: Boolean,
    val lastChecked: Instant,
    val optionsJson: String
) {
    init {
        require(name.isNotBlank()) { "name cant be blank" }
        require(platform.isNotBlank()) { "platform cant be blank" }
        require(feedUrl.isNotBlank()) { "feedUrl cant be blank" }
        require(folderName.isNotBlank()) { "folderName cant be blank" }
    }
}