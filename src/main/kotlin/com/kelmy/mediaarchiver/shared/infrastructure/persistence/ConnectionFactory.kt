package com.kelmy.mediaarchiver.shared.infrastructure.persistence

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

/**
 * Creates JDBC connections to the local SQLite database.
 *
 * A **new** connection is returned on every call and the caller is responsible for closing it
 * (see [SqlExecutor.execute]). Connections are never shared between threads. Foreign keys are
 * enabled on every connection.
 */
internal object ConnectionFactory {
    private const val DEFAULT_URL = "jdbc:sqlite:mediaarchiver.db"

    /** JDBC URL used to open connections. Overridable so tests can point to another database. */
    var url: String = DEFAULT_URL

    /**
     * Opens a new connection to [url].
     *
     * @throws SQLException if the connection cannot be established.
     */
    @JvmStatic
    @Throws(SQLException::class)
    internal fun getConnection(): Connection {
        val connection = DriverManager.getConnection(url)
        // SQLite ignores FOREIGN KEY constraints unless they are enabled on every connection.
        connection.createStatement().use { it.execute("PRAGMA foreign_keys = ON") }
        return connection
    }
}
