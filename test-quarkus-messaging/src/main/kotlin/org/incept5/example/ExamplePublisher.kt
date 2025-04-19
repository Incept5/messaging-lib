package org.incept5.example

import org.incept5.messaging.pub.MessagePublisher
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class ExamplePublisher(val messagePublisher: MessagePublisher) {
    fun publish(msg: String, count: Int = 0) {
        messagePublisher.publish("example-topic", ExamplePayload(msg, count))
    }
}