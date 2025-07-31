
# Incept5 Messaging Library - Retry Mechanism Developer Guide

## Overview

The Incept5 Messaging Library provides a robust retry mechanism for handling failed message processing. When exceptions are thrown during message processing in `TopicSubscriber.onPayload()` or `MessageSubscriber.onMessage()`, the library can automatically retry the message based on exception classification and configuration.

## How Retries Work

### Message Processing Flow

1. **Message Dispatch**: `LocalMessageDispatchTask` fetches a message and calls `MessageDispatchingService.dispatchMessageToSubscribers()`
2. **Subscriber Processing**: Each matching subscriber's `onMessage()` or `onPayload()` method is called
3. **Exception Handling**: If an exception occurs, it bubbles up to `LocalMessageDispatchTask.apply()`
4. **Retry Decision**: The task checks if the exception is retryable using `exception.isRetryable()`
5. **Transaction Rollback**: All exceptions cause `tm.setRollbackOnly()` to rollback the transaction
6. **Task Conclusion**: 
   - Retryable exceptions return `TaskConclusion.INCOMPLETE` (triggers retry)
   - Non-retryable exceptions are re-thrown (task fails permanently)

### Exception Classification

The library distinguishes between two types of exceptions:

- **Retryable Exceptions**: Transient failures that may succeed on retry (network timeouts, temporary database locks, etc.)
- **Non-Retryable Exceptions**: Permanent failures that won't succeed on retry (validation errors, business logic violations, etc.)

## Making Exceptions Retryable

### Using addMetadata() Extension

To make an exception retryable, use the `addMetadata()` extension function:

```kotlin
@ApplicationScoped
class OrderProcessingSubscriber : TopicSubscriber<OrderPayload>("order-processing", OrderPayload::class) {
    
    override fun onPayload(payload: OrderPayload) {
        try {
            processOrder(payload)
        } catch (e: DatabaseConnectionException) {
            // This is a transient error - retry it
            throw e.addMetadata(
                ErrorCategory.INFRASTRUCTURE, 
                Error("database-connection-failed"), 
                retryable = true
            )
        } catch (e: InvalidOrderException) {
            // This is a permanent error - don't retry
            throw e.addMetadata(
                ErrorCategory.VALIDATION, 
                Error("invalid-order"), 
                retryable = false  // or omit this parameter (defaults to false)
            )
        }
    }
    
    private fun processOrder(payload: OrderPayload) {
        // Order processing logic
    }
}
```

### Example Scenarios

#### Retryable Exceptions (Should Retry)
```kotlin
// Network timeouts
throw SocketTimeoutException("Service unavailable").addMetadata(
    ErrorCategory.INFRASTRUCTURE, 
    Error("service-timeout"), 
    retryable = true
)

// Database lock conflicts
throw SQLException("Deadlock detected").addMetadata(
    ErrorCategory.CONFLICT, 
    Error("database-deadlock"), 
    retryable = true
)

// External service temporarily unavailable
throw ServiceUnavailableException("Payment service down").addMetadata(
    ErrorCategory.INFRASTRUCTURE, 
    Error("payment-service-unavailable"), 
    retryable = true
)
```

#### Non-Retryable Exceptions (Should Not Retry)
```kotlin
// Validation errors
throw IllegalArgumentException("Invalid email format").addMetadata(
    ErrorCategory.VALIDATION, 
    Error("invalid-email")
    // retryable defaults to false
)

// Business rule violations
throw BusinessRuleException("Order total exceeds credit limit").addMetadata(
    ErrorCategory.BUSINESS_LOGIC, 
    Error("credit-limit-exceeded")
)

// Authentication/Authorization failures
throw UnauthorizedException("Invalid API key").addMetadata(
    ErrorCategory.SECURITY, 
    Error("invalid-credentials")
)
```

## Retry Configuration

### Default Configuration

By default, the library retries failed messages using exponential backoff over the course of 1 week:
- **Initial retry interval**: 1 second
- **Backoff multiplier**: 1.5x
- **Maximum retries**: 32
- **Total retry period**: ~1 week

### Custom Configuration

Override retry behavior in your `application.yaml`:

```yaml
task:
  scheduler:
    tasks:
      local-message-dispatch-task:
        onIncomplete:
          retryInterval: PT30S     # Initial retry interval (30 seconds)
          retryExponent: 2.0       # Backoff multiplier (double each time)
          maxRetries: 10           # Maximum retry attempts
```

### Configuration Examples

#### Aggressive Retry (Fast, Short Duration)
```yaml
task:
  scheduler:
    tasks:
      local-message-dispatch-task:
        onIncomplete:
          retryInterval: PT5S      # Start with 5 seconds
          retryExponent: 1.5       # Moderate backoff
          maxRetries: 8            # Retry for ~15 minutes total
```

#### Conservative Retry (Slow, Long Duration)
```yaml
task:
  scheduler:
    tasks:
      local-message-dispatch-task:
        onIncomplete:
          retryInterval: PT5M      # Start with 5 minutes
          retryExponent: 1.2       # Gentle backoff
          maxRetries: 50           # Retry for several days
```

#### Linear Retry (No Backoff)
```yaml
task:
  scheduler:
    tasks:
      local-message-dispatch-task:
        onIncomplete:
          retryInterval: PT1M      # 1 minute intervals
          retryExponent: 1.0       # No backoff (linear)
          maxRetries: 60           # Retry for 1 hour
```

## Transaction Behavior

### Automatic Rollback

**Important**: All exceptions (retryable or not) cause transaction rollback via `tm.setRollbackOnly()`.

```kotlin
@ApplicationScoped
class UserRegistrationSubscriber : TopicSubscriber<UserPayload>("user-registration", UserPayload::class) {
    
    @Inject
    lateinit var userRepository: UserRepository
    
    override fun onPayload(payload: UserPayload) {
        // This will be saved to the database
        val user = userRepository.save(User(payload.email, payload.name))
        
        // If this throws a retryable exception, 
        // the user save above will be rolled back
        sendWelcomeEmail(user)
    }
    
    private fun sendWelcomeEmail(user: User) {
        try {
            emailService.send(user.email, "Welcome!")
        } catch (e: EmailServiceException) {
            // This will cause the entire transaction to rollback
            // The user won't be saved to the database
            throw e.addMetadata(
                ErrorCategory.INFRASTRUCTURE, 
                Error("email-service-failed"), 
                retryable = true
            )
        }
    }
}
```

### Testing Transaction Rollback

The library includes comprehensive tests demonstrating transaction rollback:

```kotlin
@Test
fun testRetryRollsback() {
    val payload = ExamplePayload("retry", 3)
    publisher.publish("transaction-topic", payload)
    
    // Wait for first retry attempt
    Awaitility.await().atMost(Duration.ofSeconds(10)).until {
        TransactionTopicSubscriber.getCount("retry") == 1L
    }
    
    // Verify that database changes were rolled back
    assert(repo.findById(payload.id) == null)
    
    // Wait for successful processing after retries
    Awaitility.await().atMost(Duration.ofSeconds(10)).until {
        TransactionTopicSubscriber.getCount("retry") == 3L
    }
    
    // Verify that database changes are now committed
    assert(repo.findById(payload.id) != null)
}
```

## Best Practices

### 1. Exception Classification Strategy

```kotlin
@ApplicationScoped
class PaymentProcessingSubscriber : TopicSubscriber<PaymentPayload>("payment-processing", PaymentPayload::class) {
    
    override fun onPayload(payload: PaymentPayload) {
        try {
            processPayment(payload)
        } catch (e: Exception) {
            when (e) {
                // Transient infrastructure issues - retry
                is SocketTimeoutException,
                is ConnectException,
                is SQLException -> throw e.addMetadata(
                    ErrorCategory.INFRASTRUCTURE,
                    Error("infrastructure-failure"),
                    retryable = true
                )
                
                // Business validation errors - don't retry
                is ValidationException,
                is IllegalArgumentException -> throw e.addMetadata(
                    ErrorCategory.VALIDATION,
                    Error("validation-failed"),
                    retryable = false
                )
                
                // Unknown errors - be conservative, don't retry
                else -> throw e.addMetadata(
                    ErrorCategory.UNKNOWN,
                    Error("unknown-error"),
                    retryable = false
                )
            }
        }
    }
}
```

### 2. Idempotent Message Processing

Since messages may be retried, ensure your processing is idempotent:

```kotlin
@ApplicationScoped
class OrderFulfillmentSubscriber : TopicSubscriber<OrderPayload>("order-fulfillment", OrderPayload::class) {
    
    override fun onPayload(payload: OrderPayload) {
        // Check if already processed (idempotency)
        if (orderRepository.isAlreadyFulfilled(payload.orderId)) {
            logger.info("Order ${payload.orderId} already fulfilled, skipping")
            return
        }
        
        try {
            fulfillOrder(payload)
            orderRepository.markAsFulfilled(payload.orderId)
        } catch (e: FulfillmentException) {
            throw e.addMetadata(
                ErrorCategory.INFRASTRUCTURE,
                Error("fulfillment-failed"),
                retryable = true
            )
        }
    }
}
```

### 3. Monitoring and Alerting

Add logging and metrics for retry scenarios:

```kotlin
@ApplicationScoped
class MonitoredSubscriber : TopicSubscriber<PaymentPayload>("payments", PaymentPayload::class) {
    
    companion object {
        private val logger = LoggerFactory.getLogger(MonitoredSubscriber::class.java)
        private val retryCounter = Counter.builder("message_retries_total")
            .description("Total number of message processing retries")
            .register(Metrics.globalRegistry)
    }
    
    override fun onPayload(payload: PaymentPayload) {
        try {
            processPayment(payload)
        } catch (e: Exception) {
            logger.warn("Payment processing failed for ${payload.paymentId}, will retry", e)
            retryCounter.increment(
                Tags.of(
                    "topic", "payments",
                    "error_type", e.javaClass.simpleName
                )
            )
            
            throw e.addMetadata(
                ErrorCategory.INFRASTRUCTURE,
                Error("payment-processing-failed"),
                retryable = true
            )
        }
    }
}
```

### 4. Graceful Degradation

Consider implementing circuit breaker patterns for external services:

```kotlin
@ApplicationScoped
class ResilientSubscriber : TopicSubscriber<NotificationPayload>("notifications", NotificationPayload::class) {
    
    private val circuitBreaker = CircuitBreaker.ofDefaults("email-service")
    
    override fun onPayload(payload: NotificationPayload) {
        try {
            // Use circuit breaker to prevent cascading failures
            circuitBreaker.executeSupplier {
                emailService.send(payload.email, payload.message)
            }
        } catch (e: CallNotPermittedException) {
            // Circuit breaker is open - don't retry immediately
            logger.warn("Email service circuit breaker is open, will retry later")
            throw RuntimeException("Email service unavailable").addMetadata(
                ErrorCategory.INFRASTRUCTURE,
                Error("email-service-circuit-open"),
                retryable = true
            )
        } catch (e: Exception) {
            throw e.addMetadata(
                ErrorCategory.INFRASTRUCTURE,
                Error("email-service-failed"),
                retryable = true
            )
        }
    }
}
```

## Common Pitfalls to Avoid

### 1. Don't Retry Validation Errors
```kotlin
// ❌ Wrong - will retry forever
override fun onPayload(payload: UserPayload) {
    if (payload.email.isBlank()) {
        throw IllegalArgumentException("Email is required").addMetadata(
            ErrorCategory.VALIDATION,
            Error("missing-email"),
            retryable = true  // ❌ This will never succeed
        )
    }
}

// ✅ Correct - validation errors should not retry
override fun onPayload(payload: UserPayload) {
    if (payload.email.isBlank()) {
        throw IllegalArgumentException("Email is required").addMetadata(
            ErrorCategory.VALIDATION,
            Error("missing-email")
            // retryable = false (default)
        )
    }
}
```

### 2. Consider Side Effects in Retries
```kotlin
// ❌ Problematic - may send multiple emails
override fun onPayload(payload: OrderPayload) {
    emailService.sendOrderConfirmation(payload.customerEmail)  // Side effect!
    
    if (someConditionFails()) {
        throw RuntimeException("Processing failed").addMetadata(
            ErrorCategory.INFRASTRUCTURE,
            Error("processing-failed"),
            retryable = true  // This will resend the email on retry
        )
    }
}

// ✅ Better - make operations idempotent
override fun onPayload(payload: OrderPayload) {
    if (!emailService.wasConfirmationSent(payload.orderId)) {
        emailService.sendOrderConfirmation(payload.customerEmail)
        emailService.markConfirmationSent(payload.orderId)
    }
    
    if (someConditionFails()) {
        throw RuntimeException("Processing failed").addMetadata(
            ErrorCategory.INFRASTRUCTURE,
            Error("processing-failed"),
            retryable = true
        )
    }
}
```

### 3. Don't Ignore Transaction Rollback
```kotlin
// ❌ Wrong - assumes data is saved even on exception
override fun onPayload(payload: UserPayload) {
    val user = userRepository.save(User(payload))  // This will be rolled back!
    
    try {
        externalService.notifyUserCreated(user.id)
    } catch (e: ServiceException) {
        // Even though we think the user is saved, 
        // it will be rolled back due to this exception
        throw e.addMetadata(
            ErrorCategory.INFRASTRUCTURE,
            Error("notification-failed"),
            retryable = true
        )
    }
}

// ✅ Correct - understand transaction boundaries
override fun onPayload(payload: UserPayload) {
    // Both operations succeed or both are rolled back
    val user = userRepository.save(User(payload))
    
    try {
        externalService.notifyUserCreated(user.id)
    } catch (e: ServiceException) {
        logger.warn("Failed to notify external service, user creation will be retried")
        throw e.addMetadata(
            ErrorCategory.INFRASTRUCTURE,
            Error("notification-failed"),
            retryable = true
        )
    }
}
```

## Debugging and Monitoring

### Log Messages for Retry Scenarios

The library automatically logs retry attempts:

```
WARN  [LocalMessageDispatchTask] Retryable Exception - Will Rollback and Retry : 12345678-1234-1234-1234-123456789012
ERROR [LocalMessageDispatchTask] Non-Retryable Exception Encountered : 12345678-1234-1234-1234-123456789012
```

### Monitoring Retry Metrics

Consider adding these metrics to monitor retry behavior:

- `message_processing_retries_total` - Total number of retries
- `message_processing_failures_total` - Total permanent failures
- `message_processing_success_after_retry_total` - Messages that succeeded after retry
- `message_retry_duration_seconds` - Time spent in retry cycles

## Conclusion

The Incept5 Messaging Library provides a robust retry mechanism that:

1. **Automatically handles retries** based on exception classification
2. **Ensures transactional consistency** with automatic rollback
3. **Provides flexible configuration** for different retry strategies
4. **Supports both retryable and non-retryable exceptions**

By following the patterns and best practices outlined in this guide, you can build resilient message processing systems that gracefully handle failures and recover from transient issues while avoiding infinite retry loops on permanent failures.
