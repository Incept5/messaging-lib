package org.incept5.messaging.pub

import org.incept5.messaging.Message

/**
 * Publish a Message to the Messaging system
 *
 * Each message is published to a single topic and other components
 * subscribe to the topic to receive the message.
 *
 */
interface MessagePublisher {

    fun publish(message: Message)

    fun publish(topic: String, payload: Any) {
        publish(Message(topic, payload))
    }
}