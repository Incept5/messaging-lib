# Incept5 Messaging Lib

## Description

This library provides a set of classes that allow services to communicate via asynchronous messaging.
It builds on top of the scheduler-lib library that is used for local message dispatching so a scheduled
jobbing task is
In the future we can add additional message processors that can export some or all messages out
to kafka or another broker etc.


## Usage

First you need to include the messaging-quarkus library in your project:

    implementation("com.github.incept5.messagimg-lib:messaging-quarkus:1.0.X")

Add scheduler and messaging to flyway locations in application.yaml:

    quarkus:
      flyway:
        locations: db/migration,db/scheduler,db/messaging

Also tell scheduler about your schema:

    task:
      scheduler:
        schema: ${quarkus.flyway.default-schema}

### Simple Pub/Sub via topic name

And then you can inject the MessagePublisher into your service and use it to send messages:

    @ApplicationScoped
    class ExampleService(val publisher: MessagePublisher) {
        
        @Transactional
        fun someMethod(msg: String) {
            publisher.publish("example-topic", ExamplePayload(msg))
        }
    }

And then some other component can subscribe to the topic and be notified to do something for each typed payload:

    @ApplicationScoped
    class ExampleSubscriber : TopicSubscriber<ExamplePayload>("example-topic", ExamplePayload::class) {

        override fun onPayload(payload: ExamplePayload) {
            // do something with the payload
        }
    }

If you want to marshal the payload into a class that is not the same as the original type (payload class) you can just override matchType like:

    @ApplicationScoped
    class ExampleSubscriber : TopicSubscriber<DifferentPayload>("example-topic", DifferentPayload::class) {

        // original source type was ExamplePayload but we want to marshal the json into a DifferentPayload object instead
        override fun matchType(message: Message): Boolean {
            return message.type.contains("ExamplePayload")
        }

        override fun onPayload(payload: DifferentPayload) {
            // do something with the payload
        }
    }

### More control via Message Subscriber

You can be more flexible about which messages you handle and how they are processed for example
here we are handling different payload types in different ways on any topic that contains "foobar":

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

    

# Configuration

By default, the locally dispatched tasks that throw an exception that is marked as retryable will be retried upto 32 times 
over the course of a week using an exponential back off strategy.  
You can override this behaviour by adding the following to your application.yaml:

    task:
      scheduler:
        tasks:
          local-message-dispatch-task:
            on-failure:
              max-retry: 3
              retry-interval: PT1M
              retry-exponent: 1.0

# Modules

This library is made up of the following modules:

### messaging-core

This module contains the core messaging classes and interfaces including:

- Message - the data class that is used to send messages
- MessagePublisher - the interface that is used to publish messages to topics
- TopicSubscriber - the abstract class that is used to subscribe to topics and receive messages

It also contains an SQL based implmentation of the MessageRepository interface that is used to store messages in a database.

### scheduler-quarkus

This is a quarkus compatible library that will install the messaging services into the CDI context.

Note that you will need to use flyway to install the db-scheduler tables in your database.

### test-quarkus-messaging

This is an example Quarkus app and integration tests that check correct operation of the messaging-quarkus library.


