package com.kelmy.mediaarchiver.core.application

import com.kelmy.mediaarchiver.core.domain.port.Extractor
import com.kelmy.mediaarchiver.core.domain.model.ArchiveStatus
import com.kelmy.mediaarchiver.core.domain.model.DownloadHistory
import com.kelmy.mediaarchiver.core.domain.model.MediaItem
import com.kelmy.mediaarchiver.core.domain.port.CommandExecutor
import com.kelmy.mediaarchiver.core.domain.port.DownloadHistoryRepository
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.util.Collections
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.name


/**
 * Default [MediaDownloader].
 *
 * It runs the extractor's download command, finds the resulting `.mkv` file and saves a
 * [DownloadHistory] record. The item is only considered downloaded if the command succeeds,
 * the file is found **and** the record is saved.
 *
 * @property downloadHistoryRepo history where successful downloads are recorded.
 * @property commandExecutor     runs the download command.
 * @property clock               time source for `downloadedAt` (injectable for tests).
 */
class DefaultMediaDownloader(
    private val downloadHistoryRepo: DownloadHistoryRepository,
    private val commandExecutor: CommandExecutor,
    private val clock: Clock = Clock.systemUTC()
) : MediaDownloader {
    companion object {
        private val log = LoggerFactory.getLogger(DefaultMediaDownloader::class.java)

        /** A download is aborted if the process prints nothing for this long. */
        private const val DOWNLOAD_IDLE_TIMEOUT_SECONDS = 300L
    }

    override fun downloadAndRecord(
        item: MediaItem,
        platform: String,
        extractor: Extractor,
        outputDir: Path,
        archivePath: Path?,
        cookiesPath: Path?
    ): Boolean {
        val downloadCommand = extractor.getDownloadCommand(
            item.url, outputDir = outputDir, archivePath = archivePath,
            cookiesPath = cookiesPath
        )

        val printedMkvPaths: MutableList<String> = Collections.synchronizedList(mutableListOf())

        return try {
            val result = commandExecutor.execute(
                command = downloadCommand,
                timeoutSeconds = null,
                idleTimeoutSeconds = DOWNLOAD_IDLE_TIMEOUT_SECONDS,
                captureStdout = false,
                onStdoutLine = { line ->
                    log.debug("[yt-dlp id=${item.id}] $line")

                    val trimmed = line.trim()
                    if (trimmed.endsWith(".mkv", ignoreCase = true)) {
                        printedMkvPaths.add(trimmed)
                    }
                })
            if (result.exitCode != 0) {
                log.warn(
                    "Download failed for item [platform=$platform, id=${item.id}, title='${item.title}'] with exitCode=${result.exitCode}"
                )
                return false
            }

            val exactFilePath = resolveMkvPath(
                printedMkvPaths = synchronized(printedMkvPaths) { printedMkvPaths.toList() },
                outputDir = outputDir,
                videoId = item.id
            )
            if (exactFilePath == null) {
                log.warn(
                    "No .mkv file found or printed for [platform=$platform, id=${item.id}] (possibly already in archive.txt or skipped). Skipping history save."
                )
                return false
            }

            val record = DownloadHistory(
                platform = platform,
                videoId = item.id,
                filePath = exactFilePath,
                sourceUrl = item.url,
                downloadedAt = Instant.now(clock),
                archiveStatus = ArchiveStatus.NONE,
                archiveUrl = null
            )

            val saved = downloadHistoryRepo.save(record)
            if (!saved) {
                log.warn("History was not saved for [platform=$platform, id=${item.id}]")
                return false
            }

            log.info("Downloaded and saved history for [platform=$platform, id=${item.id}] -> ${record.filePath}")
            true
        } catch (e: Exception) {
            log.error("Error while downloading item [platform=$platform, id=${item.id}]: ${e.message}", e)
            false
        }
    }

    /**
     * Finds the absolute path of the `.mkv` file generated for [videoId].
     *
     * Strategy:
     * 1. Look at the paths printed by yt-dlp, **last first**, and take the first one that points
     *    to an existing regular file (relative paths are resolved against [outputDir]).
     * 2. As a fallback, scan [outputDir] for an `.mkv` whose name contains `[videoId]`.
     *
     * @param printedMkvPaths trimmed stdout lines that end with `.mkv`, in print order.
     * @return the absolute path, or `null` if no file was found.
     */
    private fun resolveMkvPath(
        printedMkvPaths: List<String>,
        outputDir: Path,
        videoId: String
    ): String? {
        for (candidate in printedMkvPaths.asReversed()) {
            val path = resolveCandidatePath(candidate, outputDir)
            if (path != null && path.exists() && path.isRegularFile()) {
                return path.toAbsolutePath().toString()
            }
        }

        if (outputDir.exists() && Files.isDirectory(outputDir)) {
            Files.list(outputDir).use { stream ->
                val matchedFile = stream
                    .filter { p ->
                        p.isRegularFile() &&
                                p.name.endsWith(".mkv", ignoreCase = true) &&
                                p.name.contains("[$videoId]")
                    }
                    .findFirst()
                    .orElse(null)

                if (matchedFile != null) {
                    return matchedFile.toAbsolutePath().toString()
                }
            }
        }

        return null
    }

    /**
     * Converts a printed path into a [Path], resolving relative paths against [outputDir].
     *
     * @return the path, or `null` if [rawPath] is not a valid path for the current filesystem.
     */
    private fun resolveCandidatePath(rawPath: String, outputDir: Path): Path? {
        return try {
            val p = Path.of(rawPath)
            if (p.isAbsolute) p else outputDir.resolve(p)
        } catch (_: Exception) {
            null
        }
    }
}
