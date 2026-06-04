package org.incept5.example

import org.incept5.messaging.pub.MessagePublisher
import jakarta.transaction.Transactional
import jakarta.ws.rs.GET
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/delivery-attempt")
class DeliveryAttemptResource(val messagePublisher: MessagePublisher) {

    /**
     * Returns the comma-separated list of delivery attempts observed so far for the given msg,
     * e.g. "1,2,3,4" once all redeliveries have completed.
     */
    @GET
    @Path("/{msg}")
    @Produces(MediaType.TEXT_PLAIN)
    fun attempts(@PathParam("msg") msg: String) =
        DeliveryAttemptSubscriber.attemptsFor(msg).joinToString(",")

    @PUT
    @Path("/publish/{msg}")
    @Transactional
    fun publish(@PathParam("msg") msg: String) {
        messagePublisher.publish("delivery-attempt-topic", ExamplePayload(msg, 0))
    }
}
