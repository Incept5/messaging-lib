package org.incept5.messaging

import com.fasterxml.jackson.annotation.JsonIgnore
import org.incept5.correlation.CorrelationId
import org.incept5.correlation.TraceId
import org.incept5.json.Json
import java.time.Instant
import java.util.*
import kotlin.reflect.KClass

/**
 * Core Message - is used for all communication between modules and even within a module.
 *
 * The idea is you can send a message to a topic and the message will be routed to the appropriate place
 * which might just be locally to some other part of your module.
 *
 * Note that while type normally == payload class name it does not have to.
 *
 */
data class Message(
    val topic: String,
    val payloadJson: String,
    val type: String,
    var messageId: UUID? = UUID.randomUUID(),
    var createdAt: Instant = Instant.now(),
    var correlationId: String = CorrelationId.getId(),
    var traceId: String? = TraceId.getId(),
    var replyTo: String? = null,
)  {

    /**
     * Which delivery attempt this dispatch represents, counting the original delivery as 1.
     *
     *   1 = original delivery
     *   2 = 1st redelivery
     *   3 = 2nd redelivery
     *   N = (N-1)th redelivery
     *
     * So the "3rd redelivery" is deliveryAttempt == 4.
     *
     * This is a transient, runtime-only hint stamped by the dispatcher from the scheduler's
     * redelivery (repeat) count, and read by subscribers. It is `var` only so the dispatcher can
     * set it; subscribers should treat it as read-only. It is NOT persisted in the `messages`
     * table and is excluded from JSON serialization; messages reconstructed from the DB carry
     * the default value of 1.
     *
     * Declared in the class body rather than as a constructor parameter so it is excluded from
     * the data class's generated equals()/hashCode()/copy(): two messages that differ only in
     * deliveryAttempt remain equal (it is identity-irrelevant runtime metadata).
     */
    // @get: targets the getter so Jackson's Kotlin module honours @JsonIgnore during serialization.
    @get:JsonIgnore
    var deliveryAttempt: Int = 1

    /**
     * Convenience constructor for creating a message with a payload that is a String.
     */
    constructor(topic: String, payload: Any) : this(
        topic = topic,
        payloadJson = Json.toJson(payload),
        type = payload::class.java.name
    )

    /**
     * Re-construct the payload object from the payloadJson and type
     * (assumes type is a fully qualified class name)
     */
    @JsonIgnore
    @Suppress("UNCHECKED_CAST")
    fun <T> getPayload(): T {
        return getPayloadAs(Class.forName(type) as Class<T>)
    }

    /**
     * Marshall the payload JSON into the supplied class
     */
    @JsonIgnore
    fun <T> getPayloadAs(cls: Class<T>): T {
        return Json.fromJson(payloadJson, cls)
    }

    @JsonIgnore
    fun <T : Any> getPayloadAs(cls: KClass<T>): T {
        return getPayloadAs(cls.java)
    }
}
