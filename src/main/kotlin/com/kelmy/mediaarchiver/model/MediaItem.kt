package com.kelmy.mediaarchiver.model

/**
 * Immutable Data Transfer Object (DTO) to standardize media data
 * obtained from any platform (YouTube, Bilibili, etc.).
 *
 * @property id         Unique identifier for the video/post on the originating platform
 * @property title      Content title
 * @property url        Direct URL for accessing or downloading the video/post
 * @property extractor  Extractor identifier name (e.g., 'youtube', 'bilibili')
 * @property uploadDate Publication date (YYYYMMDD or ISO format, optional)
 */
data class MediaItem(
    val id: String,
    val title: String,
    val url: String,
    val extractor: String,
    val uploadDate: String? = null
)