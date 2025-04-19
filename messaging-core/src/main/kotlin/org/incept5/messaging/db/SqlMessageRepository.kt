package org.incept5.messaging.db

import org.incept5.messaging.Message
import org.incept5.messaging.service.MessageRepository
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.*
import javax.sql.DataSource

/**
 * SQL implementation of the MessageRepository
 *
 **/
class SqlMessageRepository(val dataSource: DataSource, val schema: String = "") : MessageRepository {

    private val tableName = if (schema.isEmpty()) "messages" else "$schema.messages"

    companion object {
        private val logger = LoggerFactory.getLogger(SqlMessageRepository::class.java)
    }

    override fun save(message: Message) {
        dataSource.connection.use { conn ->
            val sql = """
                INSERT INTO $tableName (topic, payload_json, type, message_id, created_at, correlation_id, trace_id, reply_to)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, message.topic)
                stmt.setString(2, message.payloadJson)
                stmt.setString(3, message.type)
                stmt.setObject(4, message.messageId)
                stmt.setTimestamp(5, java.sql.Timestamp.from(message.createdAt))
                stmt.setString(6, message.correlationId)
                stmt.setString(7, message.traceId ?: "")
                stmt.setString(8, message.replyTo)
                stmt.executeUpdate()
            }
        }
    }

    override fun findByMessageId(messageId: UUID): Message? {
        return dataSource.connection.use { conn ->
            val sql = "SELECT * FROM $tableName WHERE message_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setObject(1, messageId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapRowToMessage(rs) else null
                }
            }
        }
    }

    override fun deleteMessagesCreatedBefore(time: Instant) {
        dataSource.connection.use { conn ->
            val sql = "DELETE FROM $tableName WHERE created_at < ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setObject(1, Timestamp.from(time))
                val count = stmt.executeUpdate()
                if (count > 0) {
                    logger.debug("Deleted expired messages : count {}, createdBefore {}", count, time)
                }
            }
        }
    }

    private fun mapRowToMessage(rs: ResultSet): Message {
        return Message(
            topic = rs.getString("topic"),
            payloadJson = rs.getString("payload_json"),
            type = rs.getString("type"),
            messageId = rs.getObject("message_id", UUID::class.java),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            correlationId = rs.getString("correlation_id"),
            traceId = rs.getString("trace_id"),
            replyTo = rs.getString("reply_to")
        )
    }
}