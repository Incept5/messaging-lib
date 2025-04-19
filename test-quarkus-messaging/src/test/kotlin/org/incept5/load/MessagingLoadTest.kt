package org.incept5.load

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.awaitility.Awaitility
import org.junit.jupiter.api.Test
import java.time.Duration

/**
 * This test creates 10,000 messages and dispatches them via the local task scheduler
 * and it checks that all 10,000 are received by the subscriber within 10 seconds
 */
@QuarkusTest
class MessagingLoadTest {

    @Inject
    lateinit var producer: LoadProducer

    @Test
    fun testLoad() {

        val count = 10000

        producer.produceSomeLoad(count)
        Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted {
            assert(LoadSubscriber.count.get() == count)
        }
    }

}