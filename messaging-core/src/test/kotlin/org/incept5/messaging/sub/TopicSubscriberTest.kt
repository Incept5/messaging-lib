package org.incept5.messaging.sub

import org.incept5.messaging.Message
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TopicSubscriberTest {

    @Test
    fun testShouldHandleMessage() {
        val subscriber = TestTopicSubscriber("test-topic")
        
        // Create a message that matches the topic and type
        val matchingMessage = Message(
            topic = "test-topic",
            payload = TestPayload("test-data")
        )
        
        // Create a message with different topic
        val differentTopicMessage = Message(
            topic = "different-topic",
            payload = TestPayload("test-data")
        )
        
        // Create a message with different payload type
        val differentTypeMessage = Message(
            topic = "test-topic",
            payload = "string payload"
        )
        
        // Test matching
        assertTrue(subscriber.shouldHandleMessage(matchingMessage))
        
        // Test non-matching topic
        assertFalse(subscriber.shouldHandleMessage(differentTopicMessage))
        
        // Test non-matching type
        assertFalse(subscriber.shouldHandleMessage(differentTypeMessage))
    }
    
    @Test
    fun testOnMessage() {
        val subscriber = TestTopicSubscriber("test-topic")
        
        // Create a message that matches the topic and type
        val matchingMessage = Message(
            topic = "test-topic",
            payload = TestPayload("test-data")
        )
        
        // Create a message with different topic
        val differentTopicMessage = Message(
            topic = "different-topic",
            payload = TestPayload("different-data")
        )
        
        // Test that onMessage calls onPayload for matching message
        subscriber.onMessage(matchingMessage)
        assertEquals("test-data", subscriber.lastProcessedPayload?.data)
        
        // Reset the last processed payload
        subscriber.lastProcessedPayload = null
        
        // Test that onMessage doesn't call onPayload for non-matching message
        subscriber.onMessage(differentTopicMessage)
        assertEquals(null, subscriber.lastProcessedPayload)
    }
    
    @Test
    fun testCustomTopicMatching() {
        val subscriber = CustomMatchingSubscriber()
        
        // Create a message with a prefix that should match
        val matchingMessage = Message(
            topic = "prefix-something",
            payload = TestPayload("test-data")
        )
        
        // Create a message without the prefix
        val nonMatchingMessage = Message(
            topic = "something-else",
            payload = TestPayload("test-data")
        )
        
        // Test custom topic matching
        assertTrue(subscriber.shouldHandleMessage(matchingMessage))
        assertFalse(subscriber.shouldHandleMessage(nonMatchingMessage))
    }
}

data class TestPayload(val data: String)

class TestTopicSubscriber(topicName: String) : TopicSubscriber<TestPayload>(topicName, TestPayload::class) {
    var lastProcessedPayload: TestPayload? = null
    
    override fun onPayload(payload: TestPayload) {
        lastProcessedPayload = payload
    }
}

class CustomMatchingSubscriber : TopicSubscriber<TestPayload>("not-used", TestPayload::class) {
    override fun onPayload(payload: TestPayload) {
        // Not important for this test
    }
    
    // Override the shouldHandleMessage method instead of topicMatches
    override fun shouldHandleMessage(message: Message): Boolean {
        return message.topic.startsWith("prefix-") && typeMatches(message.type)
    }
}