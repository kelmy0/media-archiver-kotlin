package com.kelmy.mediaarchiver.core.domain.port

/**
 * Catalogue of the available [Extractor]s, indexed by platform name.
 */
interface ExtractorRegistry {
    /** Returns the extractor of [platform], or `null` if there is none. */
    fun find(platform: String): Extractor?

    /**
     * Returns the extractor of [platform].
     *
     * @throws IllegalArgumentException if no extractor is registered for [platform].
     */
    fun get(platform: String): Extractor

    /** Returns every registered extractor. */
    fun all(): List<Extractor>
}
