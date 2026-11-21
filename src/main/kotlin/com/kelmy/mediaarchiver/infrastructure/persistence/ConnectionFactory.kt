package com.kelmy.mediaarchiver.infrastructure.persistence

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

internal object ConnectionFactory {
    private const val DEFAULT_URL = "jdbc:sqlite:mediaarchiver.db"

    // Overridable for tests 
    var url: String = DEFAULT_URL

    @JvmStatic
    @Throws(SQLException::class)
    internal fun getConnection(): Connection = DriverManager.getConnection(url)
}
