package com.kelmy.mediaarchiver.core.application

import com.kelmy.mediaarchiver.core.domain.port.Extractor
import com.kelmy.mediaarchiver.core.domain.model.ArchiveStatus
import com.kelmy.mediaarchiver.core.domain.model.DownloadHistory
import com.kelmy.mediaarchiver.core.domain.model.MediaItem
import com.kelmy.mediaarchiver.core.domain.port.DownloadHistoryRepository
import com.kelmy.mediaarchiver.infrastructure.system.CommandRunner
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

class DefaultMediaDownloader(
    private val downloadHistoryRepo: DownloadHistoryRepository,
    private val clock: Clock = Clock.systemUTC()
) : MediaDownloader {
    companion object {
        private val log = LoggerFactory.getLogger(DefaultMediaDownloader::class.java)
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

        return try {
            val result = CommandRunner.run(
                command = downloadCommand,
                timeoutSeconds = null,
                idleTimeoutSeconds = DOWNLOAD_IDLE_TIMEOUT_SECONDS,
                onStdoutLine = { line ->
                    log.debug("[yt-dlp id=${item.id}] $line")
                })
            if (result.exitCode != 0) {
                log.warn(
                    "Download failed for item [platform=$platform, id=${item.id}, title='${item.title}'] with exitCode=${result.exitCode}"
                )
                return false
            }

            val exactFilePath = resolveMkvPath(
                stdoutLines = result.stdoutLines,
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

    private fun resolveMkvPath(
        stdoutLines: List<String>,
        outputDir: Path,
        videoId: String
    ): String? {
        val printedCandidates = stdoutLines
            .asReversed()
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.endsWith(".mkv", ignoreCase = true) }

        for (candidate in printedCandidates) {
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

    private fun resolveCandidatePath(rawPath: String, outputDir: Path): Path? {
        return try {
            val p = Path.of(rawPath)
            if (p.isAbsolute) p else outputDir.resolve(p)
        } catch (_: Exception) {
            null
        }
    }
}