package org.incept5.messaging.sub

import org.incept5.messaging.Message
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MessageDispatchingServiceTest {

    @Test
    fun testHasSubscribersForMessage() {
        // Create test subscribers
        val subscriber1 = TestSubscriber("topic1")
        val subscriber2 = TestSubscriber("topic2")
        
        // Create the dispatching service with the subscribers
        val dispatchingService = MessageDispatchingService(listOf(subscriber1, subscriber2))
        
        // Create test messages
        val message1 = Message(topic = "topic1", payload = "payload1")
        val message2 = Message(topic = "topic2", payload = "payload2")
        val message3 = Message(topic = "topic3", payload = "payload3")
        
        // Test has subscribers
        assertTrue(dispatchingService.hasSubscribersForMessage(message1))
        assertTrue(dispatchingService.hasSubscribersForMessage(message2))
        assertFalse(dispatchingService.hasSubscribersForMessage(message3))
    }
    
    @Test
    fun testDispatchMessageToSubscribers() {
        // Create test subscribers
        val subscriber1 = TestSubscriber("topic1")
        val subscriber2 = TestSubscriber("topic2")
        val subscriber3 = TestSubscriber("topic1") // Another subscriber for topic1
        
        // Create the dispatching service with the subscribers
        val dispatchingService = MessageDispatchingService(listOf(subscriber1, subscriber2, subscriber3))
        
        // Create test messages
        val message1 = Message(topic = "topic1", payload = "payload1")
        val message2 = Message(topic = "topic2", payload = "payload2")
        
        // Test dispatching message1
        dispatchingService.dispatchMessageToSubscribers(message1)
        
        // Verify that subscriber1 and subscriber3 received message1
        assertEquals(1, subscriber1.messagesReceived.size)
        assertEquals(0, subscriber2.messagesReceived.size)
        assertEquals(1, subscriber3.messagesReceived.size)
        assertEquals("topic1", subscriber1.messagesReceived[0].topic)
        assertEquals("topic1", subscriber3.messagesReceived[0].topic)
        
        // Test dispatching message2
        dispatchingService.dispatchMessageToSubscribers(message2)
        
        // Verify that subscriber2 received message2
        assertEquals(1, subscriber1.messagesReceived.size)
        assertEquals(1, subscriber2.messagesReceived.size)
        assertEquals(1, subscriber3.messagesReceived.size)
        assertEquals("topic2", subscriber2.messagesReceived[0].topic)
    }
    
    @Test
    fun testCorrelationIdAndTraceIdPropagation() {
        // Create test subscriber
        val subscriber = TestSubscriber("test-topic")
        
        // Create the dispatching service
        val dispatchingService = MessageDispatchingService(listOf(subscriber))
        
        // Create a message with correlation and trace IDs
        val message = Message(topic = "test-topic", payload = "test-payload")
        message.correlationId = "test-correlation-id"
        message.traceId = "test-trace-id"
        
        // Dispatch the message
        dispatchingService.dispatchMessageToSubscribers(message)
        
        // Verify the subscriber received the message with the correct IDs
        assertEquals(1, subscriber.messagesReceived.size)
        assertEquals("test-correlation-id", subscriber.messagesReceived[0].correlationId)
        assertEquals("test-trace-id", subscriber.messagesReceived[0].traceId)
        
        // Verify the correlation ID was captured during processing
        assertEquals("test-correlation-id", subscriber.capturedCorrelationId)
    }
}

class TestSubscriber(private val topicToHandle: String) : MessageSubscriber {
    val messagesReceived = mutableListOf<Message>()
    var capturedCorrelationId: String? = null
    
    override fun shouldHandleMessage(message: Message): Boolean {
        return message.topic == topicToHandle
    }
    
    override fun onMessage(message: Message) {
        messagesReceived.add(message)
        
        // Capture the correlation ID from the current thread context
        // This is to verify that the MessageDispatchingService properly sets it
        capturedCorrelationId = org.incept5.correlation.CorrelationId.getId()
    }
}