package org.incept5.messaging

import org.incept5.messaging.db.SqlMessageRepository
import org.incept5.messaging.pub.MessagePublisher
import org.incept5.messaging.service.MessageProcessor
import org.incept5.messaging.service.MessagePublishingService
import org.incept5.messaging.service.MessageRepository
import org.incept5.messaging.sub.MessageDispatchingService
import org.incept5.messaging.sub.MessageSubscriber
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.enterprise.inject.Produces
import org.eclipse.microprofile.config.inject.ConfigProperty
import javax.sql.DataSource

/**
 * Create Quarkus managed beans at startup for the messaging module
 *
 */
class MessagingFactory {

    @Produces
    @ApplicationScoped
    fun createMessageRepository(dataSource: DataSource, @ConfigProperty(name = "quarkus.flyway.default-schema") schema: String = ""): MessageRepository {
        return SqlMessageRepository(dataSource, schema)
    }

    @Produces
    @ApplicationScoped
    fun createMessageService(messageRepository: MessageRepository, processors: Instance<MessageProcessor>): MessagePublisher {
        val consumers = processors.stream().toList()
        return MessagePublishingService(messageRepository, consumers)
    }

    @Produces
    @ApplicationScoped
    fun createMessageDispatcher(messageSubscribers: Instance<MessageSubscriber>): MessageDispatchingService {
        val subscribers = messageSubscribers.stream().toList()
        return MessageDispatchingService(subscribers)
    }

}
