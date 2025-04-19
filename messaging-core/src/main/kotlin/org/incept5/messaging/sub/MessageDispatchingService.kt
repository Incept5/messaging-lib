package org.incept5.messaging.sub

import org.incept5.correlation.CorrelationId
import org.incept5.correlation.MDCContext
import org.incept5.messaging.Message
import org.slf4j.LoggerFactory

/**
 * Dispatch messages to the appropriate subscribers
 */
class MessageDispatchingService(val messageSubscribers: List<MessageSubscriber>) : MessageDispatcher {

    companion object {
        private val logger = LoggerFactory.getLogger(MessageDispatchingService::class.java)
    }

    /**
     * Check to see if there are any subscribers that will handle the message
     */
    override fun hasSubscribersForMessage(message: Message): Boolean {
        return messageSubscribers.any { it.shouldHandleMessage(message) }
    }

    /**
     * Dispatch the message to the appropriate subscribers
     * Set the thread name to the subscriber class name
     */
    override fun dispatchMessageToSubscribers(message: Message) {
        try{
            CorrelationId.setId(message.correlationId)
            message.traceId?.let { MDCContext.put("traceId", it) }
            if (logger.isTraceEnabled) {
                logger.trace ( "Dispatching message: {}", message )
            }
            messageSubscribers.forEach { subscriber ->
                if (subscriber.shouldHandleMessage(message)) {
                    val tn = Thread.currentThread().name
                    try{
                        Thread.currentThread().name = subscriber.javaClass.simpleName
                        subscriber.onMessage(message)
                    }
                    finally {
                        Thread.currentThread().name = tn
                    }
                }
            }
        }
        finally {
            CorrelationId.clear()
            MDCContext.remove("traceId")
        }
    }
}