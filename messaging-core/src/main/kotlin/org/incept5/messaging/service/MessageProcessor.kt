package org.incept5.messaging.service

import org.incept5.messaging.Message
import java.util.function.Consumer

/**
 * Consume a Message from the Messaging system
 */

interface MessageProcessor : Consumer<Message>