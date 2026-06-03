package org.incept5.messaging.local

import org.incept5.messaging.Message
import org.incept5.messaging.service.MessageFinder
import org.incept5.messaging.sub.MessageDispatcher
import org.incept5.scheduler.model.TaskConclusion
import org.incept5.scheduler.model.TaskContext
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import jakarta.transaction.TransactionManager
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies that the dispatch task stamps the delivery attempt onto the message before dispatch,
 * derived from the scheduler's redelivery (repeat) count (repeatCount + 1).
 */
class LocalMessageDispatchTaskTest {

    private val tm = mock<TransactionManager>()

    private fun taskFor(message: Message?): Pair<LocalMessageDispatchTask, MessageDispatcher> {
        val finder = mock<MessageFinder> { on { findByMessageId(any()) } doReturn message }
        val dispatcher = mock<MessageDispatcher>()
        return LocalMessageDispatchTask(tm, finder, dispatcher) to dispatcher
    }

    @Test
    fun testFirstAttemptStampsDeliveryAttemptOne() {
        val message = Message(topic = "t", payload = "p")
        val (task, dispatcher) = taskFor(message)

        // repeatCount 0 = no redeliveries yet = original delivery
        val context = TaskContext<UUID>(message.messageId!!).apply { repeatCount = 0 }
        val conclusion = task.apply(context)

        assertEquals(TaskConclusion.COMPLETE, conclusion)
        verify(dispatcher).dispatchMessageToSubscribers(argThat { deliveryAttempt == 1 })
    }

    @Test
    fun testThirdRedeliveryStampsDeliveryAttemptFour() {
        val message = Message(topic = "t", payload = "p")
        val (task, dispatcher) = taskFor(message)

        // repeatCount 3 = three prior redeliveries = the 3rd redelivery (4th total attempt)
        val context = TaskContext<UUID>(message.messageId!!).apply { repeatCount = 3 }
        val conclusion = task.apply(context)

        assertEquals(TaskConclusion.COMPLETE, conclusion)
        verify(dispatcher).dispatchMessageToSubscribers(argThat { deliveryAttempt == 4 })
    }

    @Test
    fun testMissingMessageCompletesWithoutDispatch() {
        val (task, dispatcher) = taskFor(null)

        val context = TaskContext<UUID>(UUID.randomUUID()).apply { repeatCount = 1 }
        val conclusion = task.apply(context)

        assertEquals(TaskConclusion.COMPLETE, conclusion)
        verify(dispatcher, never()).dispatchMessageToSubscribers(any())
    }
}
