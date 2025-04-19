package org.incept5.messaging.local

import org.incept5.messaging.MessagingConfig
import org.incept5.messaging.service.MessageRepository
import org.incept5.scheduler.config.FrequencyConfig
import org.incept5.scheduler.config.NamedTaskConfig
import org.incept5.scheduler.config.RetryConfig
import org.incept5.scheduler.config.std.FrequencyConfigData
import org.incept5.scheduler.model.NamedScheduledTask
import jakarta.enterprise.context.ApplicationScoped
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * This task will run every 10 mins and will delete messages from the local database that
 * were created more than 2 weeks ago. This is trying to balance a bloated database with being
 * able to recover from various disaster scenarios. If the db is growing too big too fast you can
 * reduce the time to expiration via config like:
 *
 * ```messaging:
 *      expire-messages-after: P7D
 * ```
 */
@ApplicationScoped
class LocalMessageExpirationTask(val repository: MessageRepository, val config: MessagingConfig) : NamedScheduledTask, NamedTaskConfig {

    override fun getName(): String {
        return "local-message-expiration-task"
    }

    override fun run() {
        val deleteBefore = Instant.now().minus(config.expireMessagesAfter().orElse(Duration.ofDays(14)))
        repository.deleteMessagesCreatedBefore(deleteBefore)
    }

    /**
     * Run every 10 minutes
     */
    override fun frequency(): Optional<FrequencyConfig> {
        return Optional.of(FrequencyConfigData(recurs = Duration.ofMinutes(10)))
    }

    /**
     * Log to error on failure - any non-retryable exception
     */
    override fun onFailure(): Optional<RetryConfig> {
        return Optional.empty()
    }

    /**
     * Exponential backoff over the course of 1 week
     */
    override fun onIncomplete(): Optional<RetryConfig> {
        return Optional.empty()
    }

}