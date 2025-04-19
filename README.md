# Incept5 Messaging Library

[![JitPack](https://jitpack.io/v/incept5/messaging-lib.svg)](https://jitpack.io/#incept5/messaging-lib)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

## Overview

The Incept5 Messaging Library provides a robust, flexible system for asynchronous messaging within Quarkus applications. It enables services to communicate via a publish-subscribe pattern, with messages persisted in a database for reliability.

Built on top of the [scheduler-lib](https://github.com/incept5/scheduler-lib), it uses scheduled tasks for local message dispatching. Future versions will support exporting messages to external message brokers like Kafka.

## Features

- **Persistent messaging**: All messages are stored in a database for reliability and auditability
- **Topic-based pub/sub**: Simple API for publishing and subscribing to named topics
- **Flexible message handling**: Support for both typed and generic message subscribers
- **Transactional support**: Messages can be published within transactions
- **Configurable retry policies**: Customize retry behavior for failed message processing
- **Quarkus integration**: Seamless integration with Quarkus applications

## Modules

The library consists of the following modules:

### messaging-core

Contains the core messaging classes and interfaces:

- `Message`: Data class representing a message with topic, payload, and metadata
- `MessagePublisher`: Interface for publishing messages to topics
- `MessageRepository`: Interface for storing and retrieving messages
- `TopicSubscriber`: Abstract class for subscribing to specific topics with typed payloads
- `MessageSubscriber`: Interface for more flexible message handling
- `SqlMessageRepository`: SQL-based implementation of the MessageRepository

### messaging-quarkus

Quarkus integration module that:

- Provides CDI beans for messaging services
- Configures database migrations for message tables
- Integrates with the scheduler-lib for message dispatching
- Handles transaction management

### test-quarkus-messaging

Example Quarkus application and integration tests demonstrating library usage.

## Installation

### Gradle (Kotlin DSL)

1. Add JitPack repository to your build file:

```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}
```

2. Add the dependency:

```kotlin
// For the complete library with Quarkus integration
implementation("com.github.incept5.messaging-lib:messaging-quarkus:1.0.x")

// Or for just the core functionality
implementation("com.github.incept5.messaging-lib:messaging-core:1.0.x")
```

### Maven

1. Add JitPack repository to your pom.xml:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

2. Add the dependency:

```xml
<!-- For the complete library with Quarkus integration -->
<dependency>
    <groupId>com.github.incept5.messaging-lib</groupId>
    <artifactId>messaging-quarkus</artifactId>
    <version>1.0.x</version>
</dependency>

<!-- Or for just the core functionality -->
<dependency>
    <groupId>com.github.incept5.messaging-lib</groupId>
    <artifactId>messaging-core</artifactId>
    <version>1.0.x</version>
</dependency>
```

## Setup

### Database Configuration

Add scheduler and messaging to Flyway locations in your `application.yaml`:

```yaml
quarkus:
  flyway:
    locations: db/migration,db/scheduler,db/messaging
```

Configure the scheduler schema:

```yaml
task:
  scheduler:
    schema: ${quarkus.flyway.default-schema}
```

## Usage

### Publishing Messages

Inject the `MessagePublisher` into your service and use it to send messages:

```kotlin
@ApplicationScoped
class ExampleService(val publisher: MessagePublisher) {
    
    @Transactional
    fun someMethod(msg: String) {
        publisher.publish("example-topic", ExamplePayload(msg))
    }
}
```

### Simple Topic Subscription

Create a subscriber for a specific topic and payload type:

```kotlin
@ApplicationScoped
class ExampleSubscriber : TopicSubscriber<ExamplePayload>("example-topic", ExamplePayload::class) {

    override fun onPayload(payload: ExamplePayload) {
        // Process the payload
        logger.info("Received message: ${payload.message}")
    }
}
```

### Custom Payload Mapping

Override `matchType` to handle payloads with a different class than the original:

```kotlin
@ApplicationScoped
class ExampleSubscriber : TopicSubscriber<DifferentPayload>("example-topic", DifferentPayload::class) {

    // Original source type was ExamplePayload but we want to marshal the JSON into a DifferentPayload object
    override fun matchType(message: Message): Boolean {
        return message.type.contains("ExamplePayload")
    }

    override fun onPayload(payload: DifferentPayload) {
        // Process the payload
    }
}
```

### Advanced Message Handling

For more control, implement the `MessageSubscriber` interface:

```kotlin
@ApplicationScoped
class ExampleMessageSubscriber : MessageSubscriber {
    override fun shouldHandleMessage(message: Message): Boolean {
        // Handle all messages for any topic that contains "foobar"
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
    
    private fun onFoo(fooPayload: FooPayload) {
        logger.info("Foo message received: ${fooPayload.data}")
    }
    
    private fun onBar(barPayload: BarPayload) {
        logger.info("Bar message received: ${barPayload.info}")
    }
}
```

## Configuration

### Retry Configuration

By default, failed message processing tasks are retried up to 32 times over a week using exponential backoff. Override this behavior in your `application.yaml`:

```yaml
task:
  scheduler:
    tasks:
      local-message-dispatch-task:
        on-failure:
          max-retry: 3              # Maximum number of retries
          retry-interval: PT1M      # Initial retry interval (1 minute)
          retry-exponent: 1.0       # Backoff exponent (1.0 = linear)
```

### Message Expiration

Configure message expiration to automatically clean up processed messages:

```yaml
task:
  scheduler:
    tasks:
      local-message-expiration-task:
        cron: "0 0 * * * ?"         # Run hourly
        params:
          expiry-days: 30           # Keep messages for 30 days
```

## Building from Source

1. Clone the repository:
   ```
   git clone https://github.com/incept5/messaging-lib.git
   cd messaging-lib
   ```

2. Build with Gradle:
   ```
   ./gradlew build
   ```

3. Install to local Maven repository:
   ```
   ./gradlew publishToMavenLocal
   ```

## Requirements

- Java 21 or higher
- Quarkus 3.x
- A relational database supported by Flyway

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.

