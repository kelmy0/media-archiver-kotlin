package com.kelmy.mediaarchiver.library.domain.port

/**
 * Port that discovers the platform id of a video from its URL.
 *
 * The library needs it to register a video by hand, but knowing how URLs of each platform look is
 * the job of the extractors (another context). This interface keeps the library from depending on them:
 * the adapter lives in the `archiving` context.
 */
interface VideoIdResolver {
    /**
     * @param platform platform identifier (e.g. `youtube`).
     * @param url      URL of a single video.
     * @return the video id, or `null` if the platform is unknown or the URL is not a video URL.
     */
    fun resolve(platform: String, url: String): String?
}
