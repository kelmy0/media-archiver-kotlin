# Media Archiver

A **Kotlin** (JVM) application for automated monitoring and archiving of media channels, focused on content preservation and preventing _lost media_.

The tool scans the configured channel endpoints, identifies only media not yet recorded in the local history (avoiding duplicates and wasted memory/bandwidth), downloads each item with metadata, subtitles and thumbnails using `yt-dlp` and `ffmpeg`, and tracks the resulting files in a local database.

---

## About the Project and Its Purpose

Many videos, animations, streams and independent content disappear from the internet due to channel removals, strikes, regional unavailability or platform shutdowns — a phenomenon known as _lost media_.

**Media Archiver** was designed to automate the local preservation of channels of interest:

- **Lightweight scanning and deduplication:** queries the channel's items and cross-checks `(platform, video_id)` against the database history before downloading, skipping anything already archived.
- **Rich preservation:** downloads videos by merging audio and video (`.mkv`), along with metadata (`.info.json`), thumbnail and subtitles when available, while also keeping an archive record (`archive.txt`) and the exact path of the generated file.
- **Execution control:** respects a per-channel _cooldown_ and updates the timestamp of the last check (`last_checked`).

---

## Architecture and Organization

The project follows **Clean Architecture**, **SOLID** and other JVM best practices, separating orchestration, collection/filtering, external process execution, platform-specific extractors and persistence:

```text
src/main/kotlin/com/kelmy/mediaarchiver
├── bootstrap/     # Composition, dependency injection and entry point (Main)
├── config/        # Loads general settings and system dependencies
├── core/          # Orchestration (Orchestrator, ChannelProcessor, MediaCollector, MediaDownloader)
├── database/      # Database connection and management
├── extractors/    # Contracts and per-platform implementations (e.g., YoutubeExtractor)
├── model/         # Domain models (ChannelModel, MediaItemModel, DownloadHistoryModel, etc.)
├── repositories/  # Data access interfaces and implementations
├── schemas/       # DDL scripts / table definitions
└── utils/         # Cross-cutting utilities and command execution (CommandRunner)
```

---

## Prerequisites and Reference Versions

Since the application runs on the **JVM** and orchestrates command-line tools via `ProcessBuilder`, the following must be installed and available on the system `PATH`:

- **JDK 26+** (tested with `java version "26.0.2"`)
- **Apache Maven** (tested with `Apache Maven 3.10.0`)
- **`yt-dlp`** (tested with `2026.10.26`): responsible for listing content, extracting metadata and performing downloads
- **`ffmpeg` + `ffprobe`** (tested with `9.0.2`): required for merging audio/video streams (generating `.mkv`), embedding subtitles/thumbnails and `yt-dlp` post-processing

> **Note on compatibility and `PATH` (Windows / Linux):** Because the code runs on the JVM and commands are built as argument lists without relying on any specific shell syntax, the workflow is cross-platform. However, on both systems (especially on Linux when downloading binaries manually), the `java`, `yt-dlp` and `ffmpeg` executables must be in a directory included in the `PATH` environment variable (e.g., `/usr/local/bin` or `~/.local/bin` on Linux); otherwise the JVM will not find the commands when invoking them.

---

## How to Run the Project (No Installer)

The project currently has no packaged installer; it can be run directly via **Maven** or from an IDE (you may need to configure environment shortcuts).

### 1. Verify the tools in your environment

Open a terminal (PowerShell/CMD on Windows or bash/zsh on Linux) and confirm the binaries are on the `PATH`:

```bash
java -version
mvn -version
yt-dlp --version
ffmpeg -version
```

### 2. Clone the project

From your working directory:

```bash
git clone https://github.com/kelmy0/media-archiver-kotlin.git
cd media-archiver-kotlin
```

### 3. Configure your channels (seed file)

Until the GUI is available (see the [Roadmap](#roadmap--next-steps)), channels are registered through a seed file. At the repository root there is a template called `seed.example.json`:

```json
{
  "channels": [
    {
      "name": "Example",
      "platform": "youtube",
      "feedUrl": "https://www.youtube.com/@example",
      "folderName": "example-channel",
      "options": {}
    },
    {
      "name": "Example 2",
      "platform": "youtube",
      "feedUrl": "https://www.youtube.com/@example2",
      "folderName": "example-channel-2",
      "options": { "fetchVideos": true, "fetchShorts": true }
    }
  ]
}
```

1. Rename `seed.example.json` to **`seed.local.json`**.
2. Edit it following the example above, with one entry per channel:

| Field        | Description                                                        |
| ------------ | ------------------------------------------------------------------ |
| `name`       | Display name of the channel                                        |
| `platform`   | Source platform (currently `youtube`)                              |
| `feedUrl`    | URL of the channel to be monitored                                 |
| `folderName` | Name of the folder where the channel's files will be stored        |
| `options`    | Optional per-channel settings (e.g., `fetchVideos`, `fetchShorts`) |

### 4. Import the channels into the database

Run the application with the `--seed` flag to load the channels from `seed.local.json`. Channels that are already registered are detected and ignored, so running it again is safe:

**Windows (PowerShell):**

```powershell
mvn compile exec:java "-Dexec.args=--seed"
```

**Linux / macOS (bash/zsh):**

```bash
mvn compile exec:java -Dexec.args="--seed"
```

### 5. Run the application

After seeding, compile and run normally from the repository root:

```bash
mvn compile exec:java
```

> The seed step only needs to be repeated when you add new channels to `seed.local.json`. Duplicates are ignored automatically.

---

## Roadmap / Next Steps

- **Integration with external archiving APIs:** send/register media and metadata to services such as the **Internet Archive (`archive.org`)**, updating `archive_status` and `archive_url` in the history.
- **Graphical user interface (GUI):** add a simple interface to make it easier to register and manage channels, view the download history and monitor executions.
- **Support for more platforms:** expand the `extractors/` layer beyond YouTube while keeping the core unchanged.
- **Authentication / Cookies:** add support for a cookies file (`cookies.txt`) for channels or media that require authentication.

---

## Academic Context

Project developed as an academic work, focused on software engineering, clean architecture, local persistence, integration with external processes on the JVM and digital media preservation.
