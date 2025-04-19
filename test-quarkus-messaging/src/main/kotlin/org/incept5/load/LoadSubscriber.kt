package org.incept5.load

import org.incept5.messaging.sub.TopicSubscriber
import jakarta.enterprise.context.ApplicationScoped
import java.util.concurrent.atomic.AtomicInteger

@ApplicationScoped
class LoadSubscriber : TopicSubscriber<LoadPayload>("load-topic", LoadPayload::class) {

    companion object {
        val count = AtomicInteger(0)
    }

    override fun onPayload(payload: LoadPayload) {
        val num = count.incrementAndGet()
        println("LoadSubscriber count is now: $num and payload was: $payload")
    }

}