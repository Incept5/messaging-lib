package org.incept5.example

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.awaitility.Awaitility
import org.hamcrest.CoreMatchers.`is`
import org.junit.jupiter.api.Test
import java.time.Duration

@QuarkusTest
class ExamplePublishingTest {

    @Test
    fun testPublishing() {

        // check count is 0
        given()
            .`when`().get("/example-publisher/hello")
            .then()
            .statusCode(200)
            .body(`is`("Count is now: 0"))

        // publish a message
        given()
            .`when`().put("/example-publisher/publish")
            .then()
            .statusCode(204)

        // wait for count to become 1
        Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted {
            given()
                .`when`().get("/example-publisher/hello")
                .then()
                .statusCode(200)
                .body(`is`("Count is now: 1"))
        }
    }

    @Test
    fun testPublishingWithRetries() {

        // check count is 0
        given()
            .`when`().get("/example-publisher/retry")
            .then()
            .statusCode(200)
            .body(`is`("Count is now: 0"))

        // publish a message
        given()
            .`when`().put("/example-publisher/publish-and-retry")
            .then()
            .statusCode(204)

        // wait for count to become 3
        Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted {
            given()
                .`when`().get("/example-publisher/retry")
                .then()
                .statusCode(200)
                .body(`is`("Count is now: 3"))
        }
    }

}