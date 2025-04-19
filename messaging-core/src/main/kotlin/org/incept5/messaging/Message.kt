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
