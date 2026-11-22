package com.kelmy.mediaarchiver.archiving.infrastructure.extractor

import com.kelmy.mediaarchiver.library.domain.model.Channel

/**
 * [com.kelmy.mediaarchiver.archiving.domain.port.Extractor] for Bilibili.
 *
 * Everything that talks to `yt-dlp` is inherited from [YtDlpExtractor]; this class only knows the
 * shape of Bilibili URLs. It has no per-channel options: a channel is scanned through a single endpoint.
 *
 * @param name platform identifier this extractor answers to (normally `bilibili`).
 */
internal class BilibiliExtractor(name: String) : YtDlpExtractor(name) {
    private companion object {
        /** Matches the slashes at the end of a URL. */
        val TRAILING_SLASHES = Regex("/+$")

        /** The root of a user page, e.g. `https://space.bilibili.com/12345` (no tab in the path). */
        val SPACE_ROOT = Regex("""https?://space\.bilibili\.com/\d+""")

        /** A Bilibili video id (`BV` followed by 10 characters), as it appears in `/video/BV...` URLs. */
        val VIDEO_ID = Regex("""\b(BV[0-9A-Za-z]{10})\b""")
    }

    /**
     * Returns the single URL to scan: the user's video tab (`<space>/video`) when the feed is a
     * user page, or the feed URL itself for anything else (collections, series, a single video...).
     */
    override fun getEndpointsToScan(model: Channel): List<String> {
        val baseUrl = model.feedUrl.trim().replace(TRAILING_SLASHES, "")
        return listOf(if (SPACE_ROOT.matches(baseUrl)) "$baseUrl/video" else baseUrl)
    }

    override fun extractVideoId(url: String): String? = VIDEO_ID.find(url)?.groupValues?.get(1)
}
