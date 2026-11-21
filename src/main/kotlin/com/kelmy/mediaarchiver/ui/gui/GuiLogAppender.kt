package com.kelmy.mediaarchiver.ui.gui

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Logback appender that forwards every log event, already formatted as a single line, to the GUI.
 *
 * Logback creates it from `logback.xml`, **by fully qualified class name**: if this class is
 * moved or renamed, the `<appender class="...">` entry must be updated, otherwise the GUI log
 * stays empty. Events are ignored while no [sink] is registered.
 */
class GuiLogAppender : AppenderBase<ILoggingEvent>() {
    companion object {
        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault())

        /**
         * Receiver of the formatted lines (`[HH:mm:ss] [LEVEL] Logger - message`). It is called from the
         * thread that logged, so the receiver must hop to the Swing thread before touching components.
         */
        @Volatile
        var sink: ((String) -> Unit)? = null
    }

    override fun append(eventObject: ILoggingEvent) {
        val currentSink = sink ?: return
        val timestamp = timeFormatter.format(Instant.ofEpochMilli(eventObject.timeStamp))
        val level = eventObject.level.levelStr
        val loggerShort = eventObject.loggerName.substringAfterLast('.')
        val line = "[$timestamp] [$level] $loggerShort - ${eventObject.formattedMessage}"

        currentSink(line)
    }
}
