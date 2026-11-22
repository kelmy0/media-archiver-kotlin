package com.kelmy.mediaarchiver.archiving.domain.model

/**
 * Platform-independent description of a media item (video, short, post...) discovered
 * while scanning a channel.
 *
 * @property id         Unique identifier of the item on the originating platform.
 * @property title      Content title.
 * @property url        Direct URL used to access or download the item.
 * @property extractor  Name of the extractor that produced the item (e.g. `youtube`).
 * @property uploadDate Publication date (`YYYYMMDD` or ISO format), when the platform provides it.
 */
data class MediaItem(
    val id: String,
    val title: String,
    val url: String,
    val extractor: String,
    val uploadDate: String? = null
)
