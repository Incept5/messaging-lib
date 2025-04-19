package org.incept5.messaging

import io.smallrye.config.ConfigMapping
import java.time.Duration
import java.util.Optional

/**
 * Config properties for the messaging system
 */
@ConfigMapping(prefix = "messaging")
interface MessagingConfig {

    /**
     * We will delete old messages from the messages table after this amount of time
     * The default is 2 weeks
     *
     */
    fun expireMessagesAfter(): Optional<Duration>

}