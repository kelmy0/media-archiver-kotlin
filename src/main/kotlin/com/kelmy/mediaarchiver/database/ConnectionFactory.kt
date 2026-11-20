package com.kelmy.mediaarchiver.database

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

object ConnectionFactory {
    private const val DEFAULT_URL = "jdbc:sqlite:mediaarchiver.db"

    // Overridable for tests 
    var url: String = DEFAULT_URL

    @JvmStatic
    @Throws(SQLException::class)
    fun getConnection(): Connection = DriverManager.getConnection(url)
}
