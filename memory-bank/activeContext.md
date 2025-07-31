
---
type: "active-context"
created_date: "2025-01-31"
last_updated: "2025-01-31"
---

# Active Context: Messaging Library Retry Analysis

## Current Analysis Status

Successfully analyzed the Incept5 Messaging Library to understand the retry mechanism for failed message processing. Key findings documented below.

## Key Discovery: How Retries Work

### 1. Exception Classification System
The library uses an `isRetryable()` extension function to determine if exceptions should be retried:
- **Retryable exceptions**: Cause task to return `TaskConclusion.INCOMPLETE`, triggering retry
- **Non-retryable exceptions**: Are re-thrown, causing task failure without retry
- **All exceptions**: Cause transaction rollback via `tm.setRollbackOnly()`

### 2. Retry Configuration Location
Found in `LocalMessageDispatchTask` - the core retry logic:
- Default: Exponential backoff over 1 week (32 retries, 1.5x multiplier, starting at 1 second)
- Configurable via `application.yaml` under `task.scheduler.tasks.local-message-dispatch-task`

### 3. Exception Handling Pattern
From `ExampleTopicSubscriber` and `TransactionTopicSubscriber`:
```kotlin
throw RuntimeException("Example conflict").addMetadata(ErrorCategory.CONFLICT, Error("example"), retryable = true)
```

## Key Implementation Points

1. **TopicSubscriber.onPayload()** exceptions are caught by `LocalMessageDispatchTask.apply()`
2. **MessageDispatchingService** does NOT handle exceptions - they bubble up
3. **Transaction rollback** occurs for ALL exceptions (retryable or not)
4. **Retry decision** based on `exception.isRetryable()` result

## Next Steps

Document the complete retry mechanism including:
- Configuration options
- Exception classification patterns
- Best practices for implementers
- Examples of retryable vs non-retryable scenarios
