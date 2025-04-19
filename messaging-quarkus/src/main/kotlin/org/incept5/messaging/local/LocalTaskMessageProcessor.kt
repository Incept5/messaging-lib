package org.incept5.messaging.local

import org.incept5.messaging.Message
import org.incept5.messaging.service.MessageProcessor
import org.incept5.messaging.sub.MessageDispatcher
import jakarta.enterprise.context.ApplicationScoped

/**
 * Process Local Messages by seeing if there is at least one local subscriber that can handle it
 * and if there is then scheduling a task to dispatch the message to the local subscribers
 *
 * This processor will get called in the same transaction as the message is persisted
 */
@ApplicationScoped
class LocalTaskMessageProcessor(
    val dispatcher: MessageDispatcher,
    val dispatchTask: LocalMessageDispatchTask
) : MessageProcessor {

    override fun accept(message: Message) {
        if (message.messageId != null && dispatcher.hasSubscribersForMessage(message)) {
            dispatchTask.scheduleJob(message.messageId!!)
        }
    }
}