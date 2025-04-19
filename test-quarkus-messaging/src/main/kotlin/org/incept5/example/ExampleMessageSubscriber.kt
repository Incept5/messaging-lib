package org.incept5.example

import org.incept5.messaging.Message
import org.incept5.messaging.sub.MessageSubscriber

class ExampleMessageSubscriber : MessageSubscriber {
    override fun shouldHandleMessage(message: Message): Boolean {
        // handle all messages for any topic that contains foobar
        return message.topic.contains("foobar")
    }
    override fun onMessage(message: Message) {
        when (message.type) {
            "foo" -> {
                onFoo(message.getPayloadAs(FooPayload::class))
            }
            "bar" -> {
                onBar(message.getPayloadAs(BarPayload::class))
            }
        }
    }
    fun onFoo(fooPayload: FooPayload) {
        println("foo message received")
    }
    fun onBar(barPayload: BarPayload) {
        println("bar message received")
    }
}
data class FooPayload(val foo: String)
data class BarPayload(val foo: String)