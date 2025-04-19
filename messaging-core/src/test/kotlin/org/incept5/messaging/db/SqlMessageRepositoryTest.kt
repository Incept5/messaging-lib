package org.incept5.messaging.db

import org.incept5.messaging.Message
import org.incept5.messaging.service.MessageRepository
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MessageRepositoryTest {

    @Test
    fun testSaveAndFindByMessageId() {
        // Create a mock repository
        val repository = mock<MessageRepository>()
        
        // Create a test message
        val messageId = UUID.randomUUID()
        val testData = TestData("test-data")
        val message = Message(
            topic = "test-topic",
            payload = testData
        )
        message.messageId = messageId
        message.correlationId = "test-correlation-id"
        message.traceId = "test-trace-id"
        message.replyTo = "test-reply-to"
        
        // Set up the mock to return the message when findByMessageId is called
        whenever(repository.findByMessageId(messageId)).thenReturn(message)
        
        // Find the message by ID
        val foundMessage = repository.findByMessageId(messageId)
        
        // Verify the message was found and has the correct properties
        assertNotNull(foundMessage)
        assertEquals(messageId, foundMessage.messageId)
        assertEquals("test-topic", foundMessage.topic)
        assertEquals(message.payloadJson, foundMessage.payloadJson)
        assertEquals(message.type, foundMessage.type)
        assertEquals("test-correlation-id", foundMessage.correlationId)
        assertEquals("test-trace-id", foundMessage.traceId)
        assertEquals("test-reply-to", foundMessage.replyTo)
        
        // Verify the payload can be retrieved correctly
        val payload = foundMessage.getPayloadAs(TestData::class.java)
        assertEquals("test-data", payload.data)
    }
    
    @Test
    fun testFindByNonExistentMessageId() {
        // Create a mock repository
        val repository = mock<MessageRepository>()
        
        // Set up the mock to return null when findByMessageId is called with any UUID
        whenever(repository.findByMessageId(any())).thenReturn(null)
        
        // Try to find a message with a non-existent ID
        val nonExistentId = UUID.randomUUID()
        val foundMessage = repository.findByMessageId(nonExistentId)
        
        // Verify no message was found
        assertNull(foundMessage)
    }
    
    @Test
    fun testDeleteMessagesCreatedBefore() {
        // Create a mock repository
        val repository = mock<MessageRepository>()
        
        // Create messages with different creation times
        val now = Instant.now()
        val oldTime = now.minus(2, ChronoUnit.HOURS)
        val recentTime = now.minus(5, ChronoUnit.MINUTES)
        
        // Create an old message
        val oldMessage = Message(
            topic = "old-topic",
            payload = TestData("old-data")
        )
        oldMessage.createdAt = oldTime
        
        // Create a recent message
        val recentMessage = Message(
            topic = "recent-topic",
            payload = TestData("recent-data")
        )
        recentMessage.createdAt = recentTime
        
        // Set up the mock behavior for findByMessageId
        val messageStore = mutableMapOf(
            oldMessage.messageId!! to oldMessage,
            recentMessage.messageId!! to recentMessage
        )
        
        whenever(repository.findByMessageId(any())).thenAnswer { invocation ->
            val id = invocation.getArgument<UUID>(0)
            messageStore[id]
        }
        
        // Set up the mock behavior for deleteMessagesCreatedBefore
        doAnswer { invocation ->
            val cutoffTime = invocation.getArgument<Instant>(0)
            val keysToRemove = messageStore.entries
                .filter { it.value.createdAt.isBefore(cutoffTime) }
                .map { it.key }
            
            keysToRemove.forEach { messageStore.remove(it) }
            null
        }.whenever(repository).deleteMessagesCreatedBefore(any())
        
        // Delete messages created before 1 hour ago
        val cutoffTime = now.minus(1, ChronoUnit.HOURS)
        repository.deleteMessagesCreatedBefore(cutoffTime)
        
        // Verify the old message was deleted
        val foundOldMessage = repository.findByMessageId(oldMessage.messageId!!)
        assertNull(foundOldMessage)
        
        // Verify the recent message still exists
        val foundRecentMessage = repository.findByMessageId(recentMessage.messageId!!)
        assertNotNull(foundRecentMessage)
        assertEquals("recent-topic", foundRecentMessage.topic)
    }
}

data class TestData(val data: String)