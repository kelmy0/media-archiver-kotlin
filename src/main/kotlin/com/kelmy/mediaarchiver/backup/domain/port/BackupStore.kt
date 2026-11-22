package com.kelmy.mediaarchiver.backup.domain.port

import com.kelmy.mediaarchiver.library.domain.model.Channel
import java.nio.file.Path

/**
 * Port that writes and reads a snapshot of the library (channels and their videos) to/from a file.
 * The file format is a detail of each implementation.
 */
interface BackupStore {
    /**
     * Writes [channels], each one with its [Channel.videos] list already filled in, to [target]
     * (replacing the file if it exists).
     *
     * @throws java.io.IOException if the file cannot be written.
     */
    fun write(target: Path, channels: List<Channel>)

    /**
     * Reads the channels stored in [source]. Each returned channel has its [Channel.videos] list filled in.
     * The ids inside the file are meaningless in this database, so they are not preserved.
     *
     * @throws java.io.IOException if the file cannot be read.
     * @throws IllegalArgumentException if the file does not have the expected format.
     */
    fun read(source: Path): List<Channel>
}
