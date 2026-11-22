package com.kelmy.mediaarchiver.archiving.infrastructure.filesystem

import com.kelmy.mediaarchiver.archiving.domain.port.ChannelFolderReader
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * [ChannelFolderReader] that reads the real file system.
 */
internal class FileSystemChannelFolderReader : ChannelFolderReader {
    private companion object {
        private val log = LoggerFactory.getLogger(FileSystemChannelFolderReader::class.java)

        /** Name of the file written by `yt-dlp --download-archive`. */
        const val ARCHIVE_FILE_NAME = "archive.txt"
    }

    /** A malformed line (one without exactly `<extractor> <id>`) is ignored; an unreadable file yields an empty set. */
    override fun readArchivedIds(channelDir: Path, platform: String): Set<String> {
        val archiveFile = channelDir.resolve(ARCHIVE_FILE_NAME)
        if (!Files.isRegularFile(archiveFile)) return emptySet()

        return try {
            Files.readAllLines(archiveFile, Charsets.UTF_8)
                .map { it.trim().split(Regex("\\s+")) }
                .filter { parts -> parts.size == 2 && parts[0].equals(platform, ignoreCase = true) }
                .mapTo(HashSet()) { parts -> parts[1] }
        } catch (e: IOException) {
            log.warn("Could not read {}: {}", archiveFile, e.message)
            emptySet()
        }
    }

    override fun findMediaFile(channelDir: Path, videoId: String): String? {
        if (!Files.isDirectory(channelDir)) return null

        return try {
            Files.list(channelDir).use { stream ->
                stream
                    .filter { Files.isRegularFile(it) }
                    .filter {
                        it.fileName.toString()
                            .let { name -> name.endsWith(".mkv", ignoreCase = true) && name.contains("[$videoId]") }
                    }
                    .findFirst()
                    .map { it.toAbsolutePath().toString() }
                    .orElse(null)
            }
        } catch (e: IOException) {
            log.warn("Could not list {}: {}", channelDir, e.message)
            null
        }
    }
}
