package org.incept5.messaging.sub

import org.incept5.messaging.Message

/**
 * Subscriber that handles messages
 *
 * The message dispatcher will call shouldHandleMessage for
 * every message it receives. If shouldHandleMessage returns
 * true, then the message dispatcher will call onMessage.
 *
 */
interface MessageSubscriber {

    /**
     * Tell the message dispatcher that you are interested in
     * the given message. You could use the message.topic and/or
     * message.type to help you decide
     * @param message
     * @return true if this subscriber should handle the message
     */
    fun shouldHandleMessage(message: Message): Boolean

    /**
     * Handle the given message
     * @param message
     */
    fun onMessage(message: Message)

}