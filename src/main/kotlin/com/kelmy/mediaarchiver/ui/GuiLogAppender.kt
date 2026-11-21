package com.kelmy.mediaarchiver.ui

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class GuiLogAppender : AppenderBase<ILoggingEvent>() {
    companion object {
        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault())

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