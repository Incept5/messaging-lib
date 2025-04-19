package org.incept5.messaging.service

import org.incept5.messaging.Message
import java.time.Instant

/**
 * Repository for storing and retrieving messages
 */
interface MessageRepository : MessageFinder {

    fun save(message: Message)
    
    fun deleteMessagesCreatedBefore(time: Instant)

}