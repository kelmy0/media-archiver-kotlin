package com.kelmy.mediaarchiver.database

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.stream.Stream

import org.slf4j.LoggerFactory

object MigrationRunner {

    private const val MIGRATIONS_DIR = "db/migrations"
    private val log = LoggerFactory.getLogger(MigrationRunner::class.java)

    private val VERSION_REGEX = Regex("""^V(\d+)__""")

    @JvmStatic
    fun runPendingMigrations() {
        createMigrationsTable()
        createMigrationsDirectory()

        try {
            ConnectionFactory.getConnection().use { conn ->
                val migrationFiles: List<Path> = Files.list(Paths.get(MIGRATIONS_DIR)).use { paths ->
                    paths.filter { it.toString().endsWith(".sql") }
                        .sorted(compareBy { getMigrationVersion(it.fileName.toString()) })
                        .toList()
                }

                for (file in migrationFiles) {
                    val fileName = file.fileName.toString()
                    val version = getMigrationVersion(fileName)
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

    private fun getMigrationVersion(filename: String): Int {
        val match = VERSION_REGEX.find(filename)
            ?: throw IllegalArgumentException(
                "Invalid migration name (must follow the pattern V1__name.sql): $filename"
            )
        return match.groupValues[1].toInt()
    }

    private fun getMigrationDescription(filename: String): String {
        return filename
            .replaceFirst(VERSION_REGEX, "")
            .removeSuffix(".sql")
            .replace("_", " ")
    }

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

    private fun isAlreadyImplemented(conn: Connection, version: Int): Boolean {
        val sql = "SELECT 1 FROM schema_migrations WHERE version = ?"
        conn.prepareStatement(sql).use { pstmt ->
            pstmt.setInt(1, version)
            pstmt.executeQuery().use { rs -> return rs.next() }
        }
    }

    private fun registerMigration(conn: Connection, version: Int, description: String) {
        val sql = "INSERT INTO schema_migrations (version, description) VALUES (?,?)"
        conn.prepareStatement(sql).use { pstmt ->
            pstmt.setInt(1, version)
            pstmt.setString(2, description)
            pstmt.executeUpdate()
        }
    }
}