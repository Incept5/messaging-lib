package org.incept5.example

import org.incept5.error.Error
import org.incept5.error.ErrorCategory
import org.incept5.error.addMetadata
import org.incept5.messaging.sub.TopicSubscriber
import jakarta.enterprise.context.ApplicationScoped
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Exercises the delivery-attempt feature end-to-end: it forces retryable failures on the first
 * three attempts so the scheduler redelivers, and records the deliveryAttempt reported on each
 * dispatch. The "3rd redelivery" (deliveryAttempt == 4) triggers the STORY-21 AC5b style
 * "manual intervention required" WARN before the message finally succeeds.
 */
@ApplicationScoped
class DeliveryAttemptSubscriber : TopicSubscriber<ExamplePayload>("delivery-attempt-topic", ExamplePayload::class) {

    companion object {
        private val logger = LoggerFactory.getLogger(DeliveryAttemptSubscriber::class.java)

        // Attempts observed per message, keyed by the payload's msg field.
        private val attemptsByMsg = ConcurrentHashMap<String, MutableList<Int>>()

        fun attemptsFor(msg: String): List<Int> = attemptsByMsg[msg]?.toList() ?: emptyList()

        // Tests call this (e.g. @BeforeEach) to keep the static map from accumulating across runs.
        fun clear() = attemptsByMsg.clear()
    }

    override fun onPayload(payload: ExamplePayload) {
        // The attempt-aware overload is overridden; this should never be called.
        throw IllegalStateException("expected the delivery-attempt-aware overload to be invoked")
    }

    override fun onPayload(payload: ExamplePayload, deliveryAttempt: Int) {
        attemptsByMsg.computeIfAbsent(payload.msg) { CopyOnWriteArrayList() }.add(deliveryAttempt)

        // 3rd redelivery == 4th total attempt: flag for manual intervention.
        if (deliveryAttempt == 4) {
            logger.warn("manual intervention required for message {} (deliveryAttempt={})", payload.msg, deliveryAttempt)
        }

        // Force redelivery on the first three attempts so we can observe attempts 1..4.
        if (deliveryAttempt < 4) {
            throw RuntimeException("force redelivery")
                .addMetadata(ErrorCategory.CONFLICT, Error("delivery-attempt"), retryable = true)
        }
    }
}
