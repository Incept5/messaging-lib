package org.incept5.messaging.service

import org.incept5.messaging.Message
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import kotlin.test.Test

class MessagePublishingServiceTest {

    @Test
    fun testPublish() {
        // Create mock repository and processors
        val repository = mock<MessageRepository>()
        val processor1 = mock<MessageProcessor>()
        val processor2 = mock<MessageProcessor>()
        
        // Create the publishing service with the mocks
        val publishingService = MessagePublishingService(repository, listOf(processor1, processor2))
        
        // Create a test message
        val message = Message(topic = "test-topic", payload = "test-payload")
        
        // Publish the message
        publishingService.publish(message)
        
        // Verify the repository was called to save the message
        verify(repository).save(message)
        verifyNoMoreInteractions(repository)
        
        // Verify both processors were called with the message
        verify(processor1).accept(message)
        verify(processor2).accept(message)
        verifyNoMoreInteractions(processor1)
        verifyNoMoreInteractions(processor2)
    }
    
    @Test
    fun testPublishWithNoProcessors() {
        // Create mock repository
        val repository = mock<MessageRepository>()
        
        // Create the publishing service with no processors
        val publishingService = MessagePublishingService(repository, emptyList())
        
        // Create a test message
        val message = Message(topic = "test-topic", payload = "test-payload")
        
        // Publish the message
        publishingService.publish(message)
        
        // Verify the repository was called to save the message
        verify(repository).save(message)
        verifyNoMoreInteractions(repository)
    }
}