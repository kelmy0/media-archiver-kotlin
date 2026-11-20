package com.kelmy.mediaarchiver.utils

object ChannelValidator {
    @JvmStatic
    fun validate(name: String, platform: String, feedUrl: String, folderName: String) {
        requireNotBlank(name, "name")
        requireNotBlank(platform, "platform")
        requireNotBlank(feedUrl, "feedUrl")
        requireNotBlank(folderName, "folderName")
    }

    @JvmStatic
    private fun requireNotBlank(value: String, fieldName: String) {
        require(value.isNotBlank()) { "$fieldName cant be blank" }
    }
}