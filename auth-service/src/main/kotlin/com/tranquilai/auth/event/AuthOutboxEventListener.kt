package com.tranquilai.auth.event

import com.tranquilai.auth.service.AuthOutboxPublisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class AuthOutboxEventListener(
    private val publisher: AuthOutboxPublisher,
) {
    private val logger = LoggerFactory.getLogger(AuthOutboxEventListener::class.java)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun publishAfterCommit(event: OutboxEventQueuedEvent) {
        try {
            publisher.publishDueEvents()
        } catch (ex: Exception) {
            logger.warn("Outbox event {} was queued but immediate publish failed; scheduled retry will pick it up", event.eventId, ex)
        }
    }
}
