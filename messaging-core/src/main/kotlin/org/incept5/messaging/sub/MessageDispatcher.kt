package org.incept5.messaging.sub

import org.incept5.messaging.Message

/**
 * Message Dispatcher is responsible for propagating a message to the appropriate subscribers
 */
interface MessageDispatcher {

    fun hasSubscribersForMessage(message: Message): Boolean

    fun dispatchMessageToSubscribers(message: Message)

}