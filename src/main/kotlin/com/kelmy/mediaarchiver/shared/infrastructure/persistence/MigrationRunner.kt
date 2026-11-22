package com.kelmy.mediaarchiver.shared.infrastructure.persistence

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.Connection
import java.sql.SQLException

import org.slf4j.LoggerFactory

/**
 * Applies the SQL migrations found in `db/migrations` that were not applied yet.
 *
 * Rules:
 * - files must be named `V<number>__<description>.sql` (e.g. `V1__create_channels.sql`);
 * - they run in ascending version order, each one in its own transaction;
 * - applied versions are recorded in the `schema_migrations` table, so running it again is safe.
 *
 * Limitation: a script is split on every `;`, so a `;` inside a string literal or a trigger body
 * is not supported.
 */
internal object MigrationRunner {

    private const val MIGRATIONS_DIR = "db/migrations"
    private val log = LoggerFactory.getLogger(MigrationRunner::class.java)

    private val VERSION_REGEX = Regex("""^V(\d+)__""")

    /**
     * Creates the control table and the migrations directory if needed, then applies every pending migration.
     *
     * @throws RuntimeException if a migration file is unreadable or a migration fails
     *                          (the failed migration is rolled back).
     * @throws IllegalArgumentException if a `.sql` file does not follow the naming pattern.
     */
    @JvmStatic
    fun runPendingMigrations() {
        createMigrationsTable()
        createMigrationsDirectory()

        try {
            ConnectionFactory.getConnection().use { conn ->
                // The version is parsed once per file (not once per comparison while sorting).
                val migrationFiles: List<Pair<Int, Path>> = Files.list(Paths.get(MIGRATIONS_DIR)).use { paths ->
                    paths.filter { it.toString().endsWith(".sql") }
                        .map { getMigrationVersion(it.fileName.toString()) to it }
                        .toList()
                        .sortedBy { it.first }
                }

                for ((version, file) in migrationFiles) {
                    val fileName = file.fileName.toString()
                    val description = getMigrationDescription(fileName)

                    if (!isAlreadyImplemented(conn, version)) {
                        log.info("Running migration V{}: {}", version, description)

                        val sqlContent = Files.readString(file).removePrefix("\uFEFF")
                        executeMigration(conn, sqlContent, version, description)
                    }
                }
            }
        } catch (e: IOException) {
            throw RuntimeException("Error executing migrations", e)
        } catch (e: SQLException) {
            throw RuntimeException("Error executing migrations", e)
        }
    }

    /**
     * Runs one migration script and registers it, all inside a single transaction.
     * On any failure the transaction is rolled back and the original auto-commit mode is restored.
     */
    private fun executeMigration(conn: Connection, sqlScript: String, version: Int, description: String) {
        var originalAutoCommit = true
        try {
            originalAutoCommit = conn.autoCommit
            conn.autoCommit = false

            conn.createStatement().use { stmt ->
                sqlScript.split(";")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .forEach { statement ->
                        try {
                            stmt.execute(statement)
                        } catch (e: SQLException) {
                            log.error("Failed statement:\n{}", statement)
                            throw e
                        }
                    }
            }

            registerMigration(conn, version, description)

            conn.commit()
            log.info("Migration V{} applied and registered successfully.", version)
        } catch (e: Exception) {
            try {
                conn.rollback()
                log.error("Rollback executed successfully for failed migration V{}.", version)
            } catch (rollbackEx: SQLException) {
                log.error("Failed to execute rollback for migration V{}: {}", version, rollbackEx.message, rollbackEx)
            }

            throw RuntimeException("Failed to apply migration V$version: ${e.message}", e)
        } finally {
            try {
                conn.autoCommit = originalAutoCommit
            } catch (ex: SQLException) {
                log.error("Failed to restore original auto-commit state", ex)
            }
        }
    }

    /** Creates the migrations directory when it does not exist yet. */
    private fun createMigrationsDirectory() {
        try {
            val path = Paths.get(MIGRATIONS_DIR)
            if (!Files.exists(path)) {
                Files.createDirectories(path)
            }
        } catch (e: IOException) {
            throw RuntimeException("Could not create the migrations directory", e)
        }
    }

    /**
     * Extracts the version number from a file name such as `V3__add_index.sql` (returns `3`).
     *
     * @throws IllegalArgumentException if [filename] does not follow the `V<number>__name.sql` pattern.
     */
    private fun getMigrationVersion(filename: String): Int {
        val match = VERSION_REGEX.find(filename)
            ?: throw IllegalArgumentException(
                "Invalid migration name (must follow the pattern V1__name.sql): $filename"
            )
        return match.groupValues[1].toInt()
    }

    /** Turns `V3__add_index.sql` into a readable description (`add index`). */
    private fun getMigrationDescription(filename: String): String {
        return filename
            .replaceFirst(VERSION_REGEX, "")
            .removeSuffix(".sql")
            .replace("_", " ")
    }

    /** Creates the `schema_migrations` control table if it does not exist. */
    private fun createMigrationsTable() {
        val sql = """
            CREATE TABLE IF NOT EXISTS schema_migrations(
                version INTEGER PRIMARY KEY,
                description TEXT NOT NULL,
                applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent()

        try {
            ConnectionFactory.getConnection().use { conn ->
                conn.createStatement().use { stmt -> stmt.execute(sql) }
            }
        } catch (e: SQLException) {
            throw RuntimeException("Error creating migrations control table", e)
        }
    }

    /** Returns `true` if [version] is already registered in `schema_migrations`. */
    private fun isAlreadyImplemented(conn: Connection, version: Int): Boolean {
        val sql = "SELECT 1 FROM schema_migrations WHERE version = ?"
        conn.prepareStatement(sql).use { pstmt ->
            pstmt.setInt(1, version)
            pstmt.executeQuery().use { rs -> return rs.next() }
        }
    }

    /** Registers [version] as applied. */
    private fun registerMigration(conn: Connection, version: Int, description: String) {
        val sql = "INSERT INTO schema_migrations (version, description) VALUES (?,?)"
        conn.prepareStatement(sql).use { pstmt ->
            pstmt.setInt(1, version)
            pstmt.setString(2, description)
            pstmt.executeUpdate()
        }
    }
}
