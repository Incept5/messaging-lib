package org.incept5.example

import jakarta.transaction.Transactional
import jakarta.ws.rs.GET
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/example-publisher")
class ExamplePublishingResource(val examplePublisher: ExamplePublisher) {

    @GET
    @Path("/{msg}")
    @Produces(MediaType.TEXT_PLAIN)
    fun getCount(@PathParam("msg") msg: String) = "Count is now: ${ExampleTopicSubscriber.getCount(msg)}"

    @PUT
    @Path("/publish")
    @Transactional
    fun publishMessage() {
        examplePublisher.publish("hello")
    }

    @PUT
    @Path("/publish-and-retry")
    @Transactional
    fun publishMessageThatRetries() {
        examplePublisher.publish("retry", 3)
    }

}