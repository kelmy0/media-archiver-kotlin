package com.kelmy.mediaarchiver.infrastructure.persistence

import org.slf4j.Logger
import java.sql.Connection
import java.sql.SQLException

internal object Helpers {
    /**
     * Opens a connection, runs the action and closes it.
     * Centralizes the SQLException handling so each method doesn't repeat it.
     */
    fun <T> execute(log: Logger, errorMessage: String, action: (Connection) -> T): T {
        try {
            return ConnectionFactory.getConnection().use(action)
        } catch (e: SQLException) {
            log.error(errorMessage, e)
            throw RuntimeException(errorMessage, e)
        }
    }
}