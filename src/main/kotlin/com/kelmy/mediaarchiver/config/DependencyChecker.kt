package com.kelmy.mediaarchiver.config

import java.io.IOException
import java.util.concurrent.TimeUnit

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object DependencyChecker {
    private val log = LoggerFactory.getLogger(DependencyChecker::class.java)

    @JvmStatic
    fun validateEnviroment(): Boolean {
        log.info("Validating environment dependencies...")

        val ytdlpOk = checkCommand(
            "yt-dlp", "--version",
            "yt-dlp not found. Please install it via pip, winget, or brew."
        )
        val ffmpegOk = checkCommand(
            "ffmpeg", "-version",
            "ffmpeg not found. Required for media processing."
        )

        val nodeOk = checkCommand("node", "-v", "")
        val denoOk = checkCommand("deno", "--version", "")
        val jsRuntimeOk = nodeOk || denoOk

        if (!jsRuntimeOk) {
            log.error(
                "Neither Node.js nor Deno was found. yt-dlp requires at least one JavaScript " +
                        "runtime (Node.js or Deno) to handle media extraction properly."
            )
        } else {
            if (nodeOk) log.debug("JavaScript runtime found: Node.js")
            if (denoOk) log.debug("JavaScript runtime found: Deno")
        }

        if (!ytdlpOk || !ffmpegOk || !jsRuntimeOk) {
            log.error("The environment does not meet the minimum requirements. Essential dependencies are missing.")
            return false
        }

        log.info("All essential dependencies were successfully validated!")
        return true
    }

    private fun checkCommand(command: String, versionArgument: String, errorMessage: String): Boolean {
        return try {
            val process = ProcessBuilder(command, versionArgument)
                .redirectErrorStream(true)
                .start()

            val finished = process.waitFor(3, TimeUnit.SECONDS)

            if (!finished) {
                process.destroyForcibly()
                log.warn("Command '{}' timed out while verifying.", command)
                return false
            }

            if (process.exitValue() == 0) {
                log.debug("Dependency found: {}", command)
                true
            } else {
                if (errorMessage.isNotEmpty()) {
                    log.error("Failed to validate {}: {}", command, errorMessage)
                }
                false
            }
        } catch (e: IOException) {
            if (errorMessage.isNotEmpty()) {
                log.error("Dependency missing or inaccessible in PATH: '{}'. Reason: {}", command, errorMessage)
            } else {
                log.debug("Alternative dependency not found: '{}'", command)
            }
            false
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            log.error("Dependency verification for '{}' was interrupted.", command)
            false
        }
    }
}
