package com.kelmy.mediaarchiver.utils

object ChannelValidator {
    fun validate(name: String, platform: String, feedUrl: String, folderName: String) {
        requireNotBlank(name, "name")
        requireNotBlank(platform, "platform")
        requireNotBlank(feedUrl, "feedUrl")
        requireNotBlank(folderName, "folderName")
    }

    private fun requireNotBlank(value: String, fieldName: String) {
        require(value.isNotBlank()) { "$fieldName cant be blank" }
    }
}