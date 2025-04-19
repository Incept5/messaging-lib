package org.incept5.messaging.service

import org.incept5.messaging.Message
import org.incept5.messaging.pub.MessagePublisher
import org.slf4j.LoggerFactory

/**
 * Publish a Message to the Messaging system
 * Save the message to the repository and then pass it to all of the registered processors
 */
class MessagePublishingService(val repository: MessageRepository, val processors: List<MessageProcessor>) : MessagePublisher {

    companion object {
        private val log = LoggerFactory.getLogger(MessagePublishingService::class.java)
    }

    override fun publish(message: Message) {
        if ( log.isTraceEnabled ){
            log.trace("Message published : {}", message)
        }
        repository.save(message)
        processors.forEach { it.accept(message) }
    }
}