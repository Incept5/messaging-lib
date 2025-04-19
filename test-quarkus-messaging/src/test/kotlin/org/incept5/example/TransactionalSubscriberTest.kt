package org.incept5.example

import org.incept5.load.LoadProducer
import org.incept5.messaging.pub.MessagePublisher
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import jakarta.inject.Inject
import org.awaitility.Awaitility
import org.hamcrest.CoreMatchers.`is`
import org.junit.jupiter.api.Test
import java.time.Duration

@QuarkusTest
class TransactionalSubscriberTest {

    @Inject
    lateinit var publisher: MessagePublisher

    @Inject
    lateinit var repo: ExampleDataRepository


    @Test
    fun testRetryRollsback() {

        var payload = ExamplePayload("retry", 3)

        publisher.publish("transaction-topic", payload)

        // wait for count to become 1
        Awaitility.await().atMost(Duration.ofSeconds(10)).until {
            TransactionTopicSubscriber.getCount("retry") == 1L
        }

        // check that the repo is empty
        assert(repo.findById(payload.id) == null)

        // wait for count to become 3
        Awaitility.await().atMost(Duration.ofSeconds(10)).until {
            TransactionTopicSubscriber.getCount("retry") == 3L
        }

        // check that the repo now has the data
        assert(repo.findById(payload.id) != null)
    }

}