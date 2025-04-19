package org.incept5.load

import org.incept5.messaging.Message
import org.incept5.messaging.pub.MessagePublisher
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional

@ApplicationScoped
class LoadProducer(val publisher: MessagePublisher) {

    @Transactional
    fun produceSomeLoad(num: Int) {
        for (i in 1..num) {
            publisher.publish(Message("load-topic", LoadPayload("Load message $i")))
        }
    }
}