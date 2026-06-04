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
    fun testOnPayloadReceivesDeliveryAttempt() {
        val subscriber = DeliveryAttemptTrackingSubscriber("test-topic")

        // 3rd redelivery == deliveryAttempt 4
        val message = Message(topic = "test-topic", payload = TestPayload("test-data"))
        message.deliveryAttempt = 4

        subscriber.onMessage(message)

        assertEquals(1, subscriber.receivedAttempts.size)
        assertEquals(4, subscriber.receivedAttempts.first())
        assertEquals("test-data", subscriber.lastProcessedPayload?.data)
    }

    @Test
    fun testDeliveryAttemptDefaultsToOneWhenNotStamped() {
        val subscriber = DeliveryAttemptTrackingSubscriber("test-topic")

        // A freshly constructed message defaults to attempt 1 (original delivery)
        val message = Message(topic = "test-topic", payload = TestPayload("test-data"))

        subscriber.onMessage(message)

        assertEquals(listOf(1), subscriber.receivedAttempts)
    }

    @Test
    fun testLegacySubscriberStillReceivesPayloadRegardlessOfAttempt() {
        // A subscriber that only overrides onPayload(P) must keep working unchanged
        val subscriber = TestTopicSubscriber("test-topic")
        val message = Message(topic = "test-topic", payload = TestPayload("test-data"))
        message.deliveryAttempt = 4

        subscriber.onMessage(message)

        assertEquals("test-data", subscriber.lastProcessedPayload?.data)
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

/**
 * Overrides the delivery-attempt-aware overload to record which attempt each dispatch reports.
 */
class DeliveryAttemptTrackingSubscriber(topicName: String) : TopicSubscriber<TestPayload>(topicName, TestPayload::class) {
    var lastProcessedPayload: TestPayload? = null
    val receivedAttempts = mutableListOf<Int>()

    override fun onPayload(payload: TestPayload) {
        // Should not be called directly when the two-arg overload is overridden
        throw AssertionError("single-arg onPayload should not be invoked")
    }

    override fun onPayload(payload: TestPayload, deliveryAttempt: Int) {
        lastProcessedPayload = payload
        receivedAttempts.add(deliveryAttempt)
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