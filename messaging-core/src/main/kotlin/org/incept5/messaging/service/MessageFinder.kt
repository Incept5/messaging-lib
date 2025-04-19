package org.incept5.messaging.service

import org.incept5.messaging.Message
import java.util.UUID

/**
 * Interface for finding messages
 */
interface MessageFinder {
    fun findByMessageId(messageId: UUID): Message?
}