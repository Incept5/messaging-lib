
---
type: "project-brief"
created_date: "2025-01-31"
last_updated: "2025-01-31"
---

# Incept5 Messaging Library Analysis

## Project Overview

Analysis of the Incept5 Messaging Library's retry mechanism for failed message processing, specifically focusing on what happens when exceptions are thrown in the `onPayload()` function of `TopicSubscriber` implementations.

## Core Requirements

1. **Understand Current Retry Mechanism**: Analyze how the library currently handles message processing failures and retries
2. **Document Exception Handling**: Document what happens when subscribers throw exceptions during message processing
3. **Identify Retry Configuration**: Document how retry behavior can be configured
4. **Provide Recommendations**: Suggest improvements or best practices for handling message failures

## Key Focus Areas

- TopicSubscriber and MessageSubscriber error handling
- LocalMessageDispatchTask retry logic
- Exception classification (retryable vs non-retryable)
- Configuration options for retry behavior
- Transaction rollback behavior during retries

## Success Criteria

- Clear understanding of current retry mechanism
- Documentation of retry configuration options
- Recommendations for implementing robust error handling
- Examples of how to make exceptions retryable or non-retryable
