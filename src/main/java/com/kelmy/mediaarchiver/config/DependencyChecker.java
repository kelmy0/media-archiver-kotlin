package com.kelmy.mediaarchiver.config;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DependencyChecker {
    private static final Logger log = LoggerFactory.getLogger(DependencyChecker.class);

    private DependencyChecker() {
    }

    public static boolean validateEnviroment() {
        log.info("Validating environment dependencies...");

        boolean ytdlpOk = checkCommand("yt-dlp", "--version",
                "yt-dlp not found. Please install it via pip, winget, or brew.");
        boolean ffmpegOk = checkCommand("ffmpeg", "-version", "ffmpeg not found. Required for media processing.");

        boolean nodeOk = checkCommand("node", "-v", "");
        boolean denoOk = checkCommand("deno", "--version", "");
        boolean jsRuntimeOk = nodeOk || denoOk;

        if (!jsRuntimeOk) {
            log.error(
                    "Neither Node.js nor Deno was found. yt-dlp requires at least one JavaScript runtime (Node.js or Deno) to handle media extraction properly.");
        } else {
            if (nodeOk) {
                log.debug("JavaScript runtime found: Node.js");
            }
            if (denoOk) {
                log.debug("JavaScript runtime found: Deno");
            }
        }

        if (!ytdlpOk || !ffmpegOk || !jsRuntimeOk) {
            log.error("The environment does not meet the minimum requirements. Essential dependencies are missing.");
            return false;
        }

        log.info("All essential dependencies were successfully validated!");
        return true;
    }

    private static boolean checkCommand(String command, String versionArgument, String errorMessage) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command, versionArgument);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            boolean finished = process.waitFor(3, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                log.warn("Command '{}' timed out while verifying.", command);
                return false;
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.debug("Dependency found: {}", command);
                return true;
            } else {
                if (!errorMessage.isEmpty()) {
                    log.error("Failed to validate {}: {}", command, errorMessage);
                }
                return false;
            }
        } catch (IOException e) {
            if (!errorMessage.isEmpty()) {
                log.error("Dependency missing or inaccessible in PATH: '{}'. Reason: {}", command, errorMessage);
            } else {
                log.debug("Alternative dependency not found: '{}'", command);
            }
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Dependency verification for '{}' was interrupted.", command);
            return false;
        }
    }
}
