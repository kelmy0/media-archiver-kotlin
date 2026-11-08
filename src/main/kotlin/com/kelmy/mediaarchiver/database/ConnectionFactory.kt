package com.kelmy.mediaarchiver.database

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

object ConnectionFactory {
    private val URL = "jdbc:sqlite:mediaarchiver.db"

    @JvmStatic
    fun getConnection(): Connection {
        return try {
            DriverManager.getConnection(URL)
        } catch (e: SQLException) {
            throw RuntimeException("Error connecting to the SQLite database", e);
        }
    }
}
