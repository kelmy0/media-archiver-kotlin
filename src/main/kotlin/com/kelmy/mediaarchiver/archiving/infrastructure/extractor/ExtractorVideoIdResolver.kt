package com.kelmy.mediaarchiver.archiving.infrastructure.extractor

import com.kelmy.mediaarchiver.archiving.domain.port.ExtractorRegistry
import com.kelmy.mediaarchiver.library.domain.port.VideoIdResolver

/**
 * Adapter that lets the library discover a video id using the extractors of the archiving context.
 * It delegates to the extractor registered for the platform (**polymorphism**: the YouTube and the
 * Bilibili extractors answer differently to the same call).
 */
internal class ExtractorVideoIdResolver(private val extractors: ExtractorRegistry) : VideoIdResolver {
    override fun resolve(platform: String, url: String): String? =
        extractors.find(platform)?.extractVideoId(url)
}
