package com.kelmy.mediaarchiver.archiving.domain.port

/**
 * Port that remembers which channel folders already had their `archive.txt` imported into the
 * database, so the file is read **once** per folder and not on every run.
 */
interface ImportedFoldersRepository {
    /** Returns `true` if the folder named [folderName] was already imported. */
    fun isImported(folderName: String): Boolean

    /** Marks [folderName] as imported. Marking it again is harmless. */
    fun markImported(folderName: String)
}
