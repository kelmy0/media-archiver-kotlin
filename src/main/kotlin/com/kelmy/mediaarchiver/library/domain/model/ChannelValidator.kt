package com.kelmy.mediaarchiver.library.domain.model

/**
 * Business rules shared by [Channel] and [NewChannel].
 */
internal object ChannelValidator {
    /**
     * Checks that every mandatory text field of a channel is filled in.
     *
     * @throws IllegalArgumentException naming the first field that is blank.
     */
    @JvmStatic
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
