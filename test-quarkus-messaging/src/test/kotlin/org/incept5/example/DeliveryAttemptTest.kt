package org.incept5.example

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.awaitility.Awaitility
import org.hamcrest.CoreMatchers.`is`
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.UUID

/**
 * End-to-end verification that the delivery attempt count propagates from the scheduler's
 * redelivery (repeat) count, through LocalMessageDispatchTask, onto the Message, and into the
 * TopicSubscriber's attempt-aware callback across real redeliveries.
 */
@QuarkusTest
class DeliveryAttemptTest {

    @BeforeEach
    fun resetObservedAttempts() {
        DeliveryAttemptSubscriber.clear()
    }

    @Test
    fun testDeliveryAttemptIncrementsAcrossRedeliveries() {

        // unique key per run so observed attempts can never collide with leftover state from
        // another test method or a reused database
        val msg = "stuck-${UUID.randomUUID()}"

        // nothing observed yet
        given()
            .`when`().get("/delivery-attempt/$msg")
            .then()
            .statusCode(200)
            .body(`is`(""))

        // publish a message that fails on attempts 1..3 and succeeds on attempt 4
        given()
            .`when`().put("/delivery-attempt/publish/$msg")
            .then()
            .statusCode(204)

        // each dispatch reports an increasing attempt: original delivery (1) plus three
        // redeliveries (2, 3, 4) — the 3rd redelivery is attempt 4. The scheduler poll and
        // on-incomplete retry intervals are pinned to 1s in application.yaml, so all four
        // attempts complete in a few seconds; 30s is a generous buffer against CI load.
        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted {
            given()
                .`when`().get("/delivery-attempt/$msg")
                .then()
                .statusCode(200)
                .body(`is`("1,2,3,4"))
        }
    }
}
