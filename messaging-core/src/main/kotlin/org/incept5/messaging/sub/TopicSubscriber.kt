package org.incept5.messaging.sub

import org.incept5.messaging.Message
import kotlin.reflect.KClass

/**
 * Abstract helper class that does the normal thing of matching on the topic
 * name and type (matches payload class name) and if it does then it will
 * marshall the payloadJson into the provided class and call onPayload
 */
abstract class TopicSubscriber<P : Any>(private val topicName: String, private val payloadClass: KClass<P>) : MessageSubscriber {

    /**
     * This is where you do your work
     */
    abstract fun onPayload(payload: P)

    /**
     * By default we match on topic name equality
     */
    fun topicMatches(topic: String): Boolean {
        return topic == topicName
    }

    /**
     * By default the type must match the payload class name
     */
    fun typeMatches(type: String): Boolean {
        return type == payloadClass.qualifiedName
    }

    override fun shouldHandleMessage(message: Message): Boolean {
        return topicMatches(message.topic) && typeMatches(message.type)
    }

    override fun onMessage(message: Message) {
        if (shouldHandleMessage(message)) {
            onPayload(message.getPayloadAs(payloadClass))
        }
    }

}