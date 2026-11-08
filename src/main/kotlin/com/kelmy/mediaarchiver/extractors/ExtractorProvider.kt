package com.kelmy.mediaarchiver.extractors

interface ExtractorProvider {
    /** Returns the platform extractor, or null if it does not exist */
    fun find(platform: String): Extractor<*>?

    /** Returns the platform extractor, or throws an exception if it does not exist. */
    fun get(platform: String): Extractor<*>

    fun all(): List<Extractor<*>>
}