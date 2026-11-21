package com.kelmy.mediaarchiver.infrastructure.extractor

import com.kelmy.mediaarchiver.core.domain.port.Extractor
import com.kelmy.mediaarchiver.core.domain.port.ExtractorRegistry

/**
 * [ExtractorRegistry] that keeps the extractors in memory, indexed by [Extractor.name].
 *
 * @param extractors the extractors to register.
 * @throws IllegalArgumentException if two extractors share the same name.
 */
class InMemoryExtractorRegistry(extractors: List<Extractor>) : ExtractorRegistry {
    private val byPlatform: Map<String, Extractor>

    init {
        val duplicated = extractors.groupBy { it.name }.filterValues { it.size > 1 }.keys
        require(duplicated.isEmpty()) { "Duplicated extractor names: $duplicated" }

        byPlatform = extractors.associateBy { it.name }
    }

    override fun find(platform: String): Extractor? = byPlatform[platform]

    override fun get(platform: String): Extractor =
        find(platform) ?: throw IllegalArgumentException("No extractor registered for platform '$platform'")

    override fun all(): List<Extractor> = byPlatform.values.toList()

    companion object {
        /** Creates the registry with every extractor shipped with the application (currently only YouTube). */
        fun createDefault(): ExtractorRegistry = InMemoryExtractorRegistry(
            listOf(
                YoutubeExtractor("youtube"),
            )
        )
    }
}
