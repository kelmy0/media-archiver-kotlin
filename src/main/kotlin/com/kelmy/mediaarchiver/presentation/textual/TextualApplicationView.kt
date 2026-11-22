package com.kelmy.mediaarchiver.presentation.textual

import com.kelmy.mediaarchiver.library.domain.model.Channel
import com.kelmy.mediaarchiver.library.domain.model.NewChannel
import com.kelmy.mediaarchiver.library.domain.model.Video
import com.kelmy.mediaarchiver.presentation.AppServices
import com.kelmy.mediaarchiver.presentation.ApplicationView
import java.nio.file.Path

/**
 * Console interface: a main menu with the channel CRUD, the video CRUD, the archiving routine and
 * the save/load of all data.
 *
 * It contains **no business rule**: it reads what the user typed, calls an application service and
 * prints the result. Validation errors thrown by the services are shown as messages.
 *
 * @property services the application services.
 * @property io       console used to read and print.
 */
internal class TextualApplicationView(
    private val services: AppServices,
    private val io: ConsoleIO = ConsoleIO()
) : ApplicationView {
    private companion object {
        const val DEFAULT_BACKUP_FILE = "mediaarchiver-backup.json"
    }

    /** Shows the main menu until the user chooses to leave (or the input ends). */
    override fun show() {
        while (!io.endOfInput) {
            io.println()
            io.println("${AnsiColors.BOLD}=== Media Archiver ===${AnsiColors.RESET}")
            io.println("1) Run archiving")
            io.println("2) Channels")
            io.println("3) Videos")
            io.println("4) Save all data to a file")
            io.println("5) Load data from a file")
            io.println("0) Exit")

            when (io.ask("Option:")) {
                "1" -> guarded { services.runArchiving() }
                "2" -> channelsMenu()
                "3" -> videosMenu()
                "4" -> guarded { saveData() }
                "5" -> guarded { loadData() }
                "0" -> return
                else -> if (!io.endOfInput) io.println("Invalid option.")
            }
        }
    }

    // ------------------------------------------------------------------ channels

    private fun channelsMenu() {
        while (!io.endOfInput) {
            io.println()
            io.println("${AnsiColors.BOLD}--- Channels ---${AnsiColors.RESET}")
            io.println("1) List  2) Add  3) Edit  4) Delete  0) Back")

            when (io.ask("Option:")) {
                "1" -> guarded { listChannels() }
                "2" -> guarded { addChannel() }
                "3" -> guarded { editChannel() }
                "4" -> guarded { deleteChannel() }
                "0" -> return
                else -> if (!io.endOfInput) io.println("Invalid option.")
            }
        }
    }

    /** Prints every channel, one per line. */
    private fun listChannels() {
        val channels = services.channels.list()
        if (channels.isEmpty()) {
            io.println("No channels registered.")
            return
        }
        channels.forEach { c ->
            val status = if (c.active) "active" else "inactive"
            io.println("#${c.id}  ${c.name}  [${c.platform}, $status]  folder=${c.folderName}  ${c.feedUrl}")
        }
    }

    private fun addChannel() {
        val name = io.ask("Name:")
        val platform = askPlatform()
        val feedUrl = io.ask("Feed / channel URL:")
        val folderName = io.ask("Folder name (blank = same as the name):").ifBlank { name }

        val created = services.channels.create(NewChannel(name, platform, feedUrl, folderName))
        io.println("Channel #${created.id} created.")
    }

    /** Edits a channel; a blank answer keeps the current value. */
    private fun editChannel() {
        val channel = askChannel() ?: return

        val name = io.ask("Name [${channel.name}]:").ifBlank { channel.name }
        val platform = io.ask("Platform [${channel.platform}] (${services.platforms.joinToString(", ")}):")
            .ifBlank { channel.platform }
        require(platform in services.platforms) { "Unknown platform '$platform'" }
        val feedUrl = io.ask("Feed / channel URL [${channel.feedUrl}]:").ifBlank { channel.feedUrl }
        val folderName = io.ask("Folder name [${channel.folderName}]:").ifBlank { channel.folderName }
        val active = io.ask("Active? (y/n) [${if (channel.active) "y" else "n"}]:")
            .ifBlank { if (channel.active) "y" else "n" }
            .equals("y", ignoreCase = true)

        val updated = channel.copy(
            name = name, platform = platform, feedUrl = feedUrl, folderName = folderName, active = active
        )
        io.println(if (services.channels.update(updated)) "Channel #${channel.id} updated." else "Channel not updated.")
    }

    private fun deleteChannel() {
        val channel = askChannel() ?: return
        if (!io.confirm("Delete '${channel.name}' and the records of all its videos? (files are kept)")) return

        io.println(if (services.channels.delete(channel.id)) "Channel deleted." else "Channel not found.")
    }

    // ------------------------------------------------------------------ videos

    /** Asks for a channel and then offers the video operations on it. */
    private fun videosMenu() {
        guarded { listChannels() }
        val channel = askChannel() ?: return

        while (!io.endOfInput) {
            io.println()
            io.println("${AnsiColors.BOLD}--- Videos of '${channel.name}' ---${AnsiColors.RESET}")
            io.println("1) List  2) Associate an existing video  3) Edit  4) Delete  0) Back")

            when (io.ask("Option:")) {
                "1" -> guarded { printVideos(loadVideos(channel.id)) }
                "2" -> guarded { associateVideo(channel) }
                "3" -> guarded { editVideo(channel) }
                "4" -> guarded { deleteVideo(channel) }
                "0" -> return
                else -> if (!io.endOfInput) io.println("Invalid option.")
            }
        }
    }

    /** Loads the channel together with its videos (the aggregate) and returns the video list. */
    private fun loadVideos(channelId: Int): List<Video> =
        services.channels.findWithVideos(channelId)?.videos ?: emptyList()

    private fun printVideos(videos: List<Video>) {
        if (videos.isEmpty()) {
            io.println("No videos in this channel.")
            return
        }
        videos.forEachIndexed { index, v ->
            io.println("${index + 1}) ${v.title}  [${v.videoId}]")
            io.println("     url:  ${v.sourceUrl}")
            io.println("     file: ${v.filePath.ifBlank { "(unknown)" }}")
        }
    }

    /** Create: registers a video that already exists on disk and associates it to [channel]. */
    private fun associateVideo(channel: Channel) {
        val title = io.ask("Title:")
        val url = io.ask("Video URL:")
        val filePath = io.ask("Path of the video file:")

        val video = services.videos.associate(channel.id, title, url, filePath)
        io.println("Video '${video.title}' [${video.videoId}] associated to '${channel.name}'.")
    }

    /** Update: changes the title and/or the channel of a video (blank keeps the current value). */
    private fun editVideo(channel: Channel) {
        val video = askVideo(channel) ?: return

        val title = io.ask("Title [${video.title}]:").ifBlank { video.title }
        val channelId = io.ask("Channel id [${video.channelId}] (type 'l' to list):").let { answer ->
            when {
                answer.equals("l", ignoreCase = true) -> {
                    listChannels()
                    io.ask("Channel id [${video.channelId}]:")
                }
                else -> answer
            }
        }.ifBlank { video.channelId.toString() }
            .toIntOrNull() ?: throw IllegalArgumentException("Channel id must be a number")

        services.videos.edit(video.platform, video.videoId, title, channelId)
        io.println("Video updated.")
    }

    private fun deleteVideo(channel: Channel) {
        val video = askVideo(channel) ?: return
        if (!io.confirm("Delete the record of '${video.title}'? (the file is kept)")) return

        io.println(if (services.videos.delete(video.platform, video.videoId)) "Video deleted." else "Video not found.")
    }

    // ------------------------------------------------------------------ save / load

    private fun saveData() {
        val file = io.ask("File to write [$DEFAULT_BACKUP_FILE]:").ifBlank { DEFAULT_BACKUP_FILE }
        val count = services.backup.saveTo(Path.of(file))
        io.println("$count channel(s) and their videos saved to $file")
    }

    private fun loadData() {
        val file = io.ask("File to read:")
        require(file.isNotBlank()) { "A file name is required" }

        val report = services.backup.loadFrom(Path.of(file))
        io.println(
            "Channels: ${report.channelsAdded} added, ${report.channelsSkipped} already existed. " +
                    "Videos: ${report.videosAdded} added, ${report.videosSkipped} already existed."
        )
    }

    // ------------------------------------------------------------------ helpers

    private fun askPlatform(): String {
        val platform = io.ask("Platform (${services.platforms.joinToString(", ")}):")
        require(platform in services.platforms) { "Unknown platform '$platform'" }
        return platform
    }

    /** Asks for a channel id and returns the channel, or `null` (after telling the user) if there is none. */
    private fun askChannel(): Channel? {
        val id = io.askInt("Channel id:") ?: return null
        return services.channels.find(id) ?: run {
            io.println("There is no channel #$id.")
            null
        }
    }

    /** Lists the videos of [channel] and asks the user to pick one by its position. */
    private fun askVideo(channel: Channel): Video? {
        val videos = loadVideos(channel.id)
        printVideos(videos)
        if (videos.isEmpty()) return null

        val number = io.askInt("Video number:") ?: return null
        return videos.getOrNull(number - 1) ?: run {
            io.println("There is no video number $number.")
            null
        }
    }

    /** Runs [action] and shows any failure as a message instead of crashing the menu. */
    private fun guarded(action: () -> Unit) {
        try {
            action()
        } catch (e: IllegalArgumentException) {
            io.println("${AnsiColors.YELLOW}Invalid data: ${e.message}${AnsiColors.RESET}")
        } catch (e: Exception) {
            io.println("${AnsiColors.RED}Error: ${e.message}${AnsiColors.RESET}")
        }
    }
}
