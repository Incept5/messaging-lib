package org.incept5.messaging.local

import org.incept5.error.isRetryable
import org.incept5.messaging.service.MessageFinder
import org.incept5.messaging.sub.MessageDispatcher
import org.incept5.scheduler.config.FrequencyConfig
import org.incept5.scheduler.config.NamedTaskConfig
import org.incept5.scheduler.config.RetryConfig
import org.incept5.scheduler.config.std.RetryConfigData
import org.incept5.scheduler.model.NamedJobbingTask
import org.incept5.scheduler.model.TaskConclusion
import org.incept5.scheduler.model.TaskContext
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.TransactionManager
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

/**
 * This task will get scheduled for each message that needs to be locally dispatched
 * to (one or more) local subscriber(s) within the local Quarkus runtime context
 *
 * Default retry config is do exponential backoff over the course of 1 week:
 * 1s, 1.5x, 32 times
 * so the first retry will be in 1 second, then 1.5 seconds, then 2.25 seconds, etc
 *
 * You can override this by setting the following properties:
 *
 * task:
 *   scheduler:
 *     tasks:
 *       local-message-dispatch-task:
 *         onIncomplete:
 *           retryInterval: PT1S
 *           retryExponent: 1.0
 *           maxRetries: 600
 *           ...
 *
 */
@ApplicationScoped
class LocalMessageDispatchTask(
    private val tm: TransactionManager,
    private val messageFinder: MessageFinder,
    private val dispatcher: MessageDispatcher
) : NamedJobbingTask<UUID>("local-message-dispatch-task"), NamedTaskConfig {

    companion object {
        private val logger = LoggerFactory.getLogger(LocalMessageDispatchTask::class.java)
    }

    /**
     * Required override of the abstract NamedJobbingTask.accept(payload). Normal dispatch does
     * not flow through here: this class overrides apply() and calls the private accept overload
     * with the real delivery attempt. This entry point is the framework default and stamps the
     * original-delivery value (1); it is retained to satisfy the contract / direct invocation.
     */
    override fun accept(payload: UUID) {
        accept(payload, 1)
    }

    private fun accept(messageId: UUID, deliveryAttempt: Int) {
        val message = messageFinder.findByMessageId(messageId)
        if (message == null) {
            logger.warn ("Message Not Found : {}", messageId)
            return
        } else {
            message.deliveryAttempt = deliveryAttempt
            dispatcher.dispatchMessageToSubscribers(message)
        }
    }

    /**
     * We support Retries if the exception thrown is retryable
     * We will rollback the transaction either way
     */
    @Transactional
    override fun apply(context: TaskContext<UUID>): TaskConclusion {
        try {
            // repeatCount is the canonical redelivery counter for this task. Why repeatCount and
            // not failureCount:
            //  - Retryable failures re-schedule the task as INCOMPLETE (via setRollbackOnly),
            //    which the scheduler tracks by incrementing repeatCount.
            //  - Non-retryable failures are re-thrown and, with onFailure() empty, are TERMINAL:
            //    the task fails permanently and is never re-dispatched. They bump failureCount
            //    (the scheduler's consecutiveFailures), but there is no subsequent attempt to
            //    observe it on.
            // Each execution ends in exactly one of {COMPLETE, INCOMPLETE, thrown}, so the two
            // counters never both move for the same attempt, and only INCOMPLETE produces a
            // redelivery. failureCount is therefore always 0 at dispatch time and a mixed
            // retryable/non-retryable sequence cannot yield a later attempt. repeatCount + 1 is
            // the exact delivery attempt number (0 redeliveries => attempt 1).
            accept(context.payload, context.repeatCount + 1)
        } catch (e: Exception) {
            if (e.isRetryable()) {
                logger.warn("Retryable Exception - Will Rollback and Retry : {}", context.payload, e)
                tm.setRollbackOnly()
                return TaskConclusion.INCOMPLETE
            } else {
                logger.error("Non-Retryable Exception Encountered : {}", context.payload, e)
                throw e
            }
        } finally {
            
        }
        return TaskConclusion.COMPLETE
    }

    override fun frequency(): Optional<FrequencyConfig> {
        return Optional.empty()
    }

    /**
     * Log to error on failure - any non-retryable exception
     */
    override fun onFailure(): Optional<RetryConfig> {
        return Optional.empty()
    }

    /**
     * Exponential backoff over the course of 1 week
     */
    override fun onIncomplete(): Optional<RetryConfig> {
        return Optional.of(RetryConfigData(Duration.ofSeconds(1), 1.5, 32))
    }
}