package org.incept5.example

import org.incept5.error.Error
import org.incept5.error.ErrorCategory
import org.incept5.error.addMetadata
import org.incept5.messaging.sub.TopicSubscriber
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class ExampleTopicSubscriber : TopicSubscriber<ExamplePayload>("example-topic", ExamplePayload::class) {

    companion object {
        val payloads = mutableListOf<ExamplePayload>()
        fun getCount(msg: String) = payloads.stream().filter { it.msg == msg }.count()
    }

    override fun onPayload(payload: ExamplePayload) {
        println("ExampleTopicSubscriber received payload: $payload")
        payloads.add(payload)

        if (payload.msg == "retry") {
            val previousRetries = getCount("retry")
            println("*** Previous retries: $previousRetries")
            if (previousRetries < payload.count) {
                throw RuntimeException("Example conflict").addMetadata(ErrorCategory.CONFLICT, Error("example"), retryable = true)
            }
        }
    }
}