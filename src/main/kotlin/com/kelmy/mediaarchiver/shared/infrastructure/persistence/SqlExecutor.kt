package com.kelmy.mediaarchiver.shared.infrastructure.persistence

import org.slf4j.Logger
import java.sql.Connection
import java.sql.SQLException

/**
 * Small helper shared by the SQL repositories.
 */
internal object SqlExecutor {
    /**
     * Opens a connection, runs [action] and closes the connection afterwards.
     *
     * It centralizes the `SQLException` handling, so each repository method does not repeat it:
     * the exception is logged with [errorMessage] and rethrown as a [RuntimeException].
     *
     * @param log          logger of the calling repository.
     * @param errorMessage message used in the log and in the thrown exception.
     * @param action       work to run with the open connection.
     * @return whatever [action] returns.
     * @throws RuntimeException wrapping any [SQLException] raised while connecting or running [action].
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
