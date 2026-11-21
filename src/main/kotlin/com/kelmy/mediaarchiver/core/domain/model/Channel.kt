package com.kelmy.mediaarchiver.core.domain.model

import java.time.Duration
import java.time.Instant

data class Channel(
    val id: Int,
    val name: String,
    val platform: String,
    val feedUrl: String,
    val folderName: String,
    val active: Boolean,
    val lastChecked: Instant,
    val optionsJson: String
) {
    init {
        ChannelValidator.validate(name, platform, feedUrl, folderName)
    }

    /**
     * Returns the remaining cooldown time.
     * If the time has already expired, returns Duration.ZERO.
     */
    fun getRemainingCooldown(now: Instant, cooldownMinutes: Int): Duration {
        val cooldownDuration = Duration.ofMinutes(cooldownMinutes.toLong())
        val expirationTime = lastChecked.plus(cooldownDuration)

        if (now.isBefore(expirationTime)) {
            return Duration.between(now, expirationTime)
        }

        return Duration.ZERO
    }
}

data class NewChannel(
    val name: String,
    val platform: String,
    val feedUrl: String,
    val folderName: String,
    val active: Boolean = true,
    val optionsJson: String = "{}"
) {
    init {
        ChannelValidator.validate(name, platform, feedUrl, folderName)
    }
}
