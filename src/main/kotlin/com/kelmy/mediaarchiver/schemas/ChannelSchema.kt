package com.kelmy.mediaarchiver.schemas

import com.kelmy.mediaarchiver.utils.ChannelValidator
import java.time.Instant

/*
* Common fields to any channel on any platform
*/
data class ChannelSchema(
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
        ChannelValidator.validate(name, platform, feedUrl, folderName)
    }
}