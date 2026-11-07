package com.kelmy.mediaarchiver.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class ConnectionFactory {
    private static final String URL = "jdbc:sqlite:mediaarchiver.db";

    private ConnectionFactory() {
    }

    /**
     * Returns a connection to the SQLite database.
     * 
     * @return java.sql.Connection
     */
    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL);
        } catch (SQLException e) {
            throw new RuntimeException("Error connecting to the SQLite database", e);
        }
    }
}
