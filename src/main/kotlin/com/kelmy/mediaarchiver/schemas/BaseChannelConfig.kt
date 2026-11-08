package com.kelmy.mediaarchiver.schemas

import java.time.Instant

/*
* Common fields to any channel on any platform
*/
data class BaseChannelConfig(
    val id: Int,
    val name: String,
    val platform: String,
    val feedUrl: String,
    val folderName: String,
    val active: Boolean,
    val lastChecked: Instant = Instant.EPOCH,
    val optionsJson: String = "{}"
) {
    init {
        require(name.isNotBlank()) { "name cant be blank" }
        require(platform.isNotBlank()) { "platform cant be blank" }
        require(feedUrl.isNotBlank()) { "feedUrl cant be blank" }
        require(folderName.isNotBlank()) { "folderName cant be blank" }
    }


}