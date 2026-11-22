package com.kelmy.mediaarchiver.archiving.infrastructure.persistence

import com.kelmy.mediaarchiver.archiving.domain.port.ImportedFoldersRepository
import com.kelmy.mediaarchiver.shared.infrastructure.persistence.SqlExecutor
import org.slf4j.LoggerFactory

/**
 * SQLite implementation of [ImportedFoldersRepository], backed by the `imported_folders` table.
 */
internal class SqlImportedFolders : ImportedFoldersRepository {
    private companion object {
        private val log = LoggerFactory.getLogger(SqlImportedFolders::class.java)
    }

    override fun isImported(folderName: String): Boolean {
        val sql = "SELECT 1 FROM imported_folders WHERE folder_name = ? LIMIT 1;"

        return SqlExecutor.execute(log, "Error checking imported folder '$folderName'") { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, folderName)
                stmt.executeQuery().use { rs -> rs.next() }
            }
        }
    }

    override fun markImported(folderName: String) {
        val sql = "INSERT OR IGNORE INTO imported_folders (folder_name) VALUES (?);"

        SqlExecutor.execute(log, "Error marking folder '$folderName' as imported") { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, folderName)
                stmt.executeUpdate()
            }
        }
    }
}
