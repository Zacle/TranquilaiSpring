package com.tranquilai.auth.service

import com.tranquilai.auth.entity.OutboxEventStatus
import com.tranquilai.auth.repository.OutboxEventRepository
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthOutboxPublisher(
    private val repository: OutboxEventRepository,
    private val rabbitTemplate: RabbitTemplate,
    @param:Value("\${app.events.auth-exchange}") private val exchange: String,
    @param:Value("\${app.outbox.batch-size}") private val batchSize: Int,
    @param:Value("\${app.outbox.max-attempts}") private val maxAttempts: Int,
) {
    private val logger = LoggerFactory.getLogger(AuthOutboxPublisher::class.java)

    @Scheduled(fixedDelayString = "\${app.outbox.publish-delay-ms}")
    @Transactional
    fun publishDueEvents() {
        val now = System.currentTimeMillis()
        val events =
            try {
                repository.findDueEventsForUpdate(
                    OutboxEventStatus.PENDING,
                    now,
                    PageRequest.of(0, batchSize),
                )
            } catch (ex: Exception) {
                logger.warn("Could not load due auth outbox events", ex)
                return
            }

        events.forEach { event ->
            try {
                rabbitTemplate.convertAndSend(exchange, event.routingKey, event.payload)
                event.markPublished()
            } catch (ex: Exception) {
                val message = ex.message ?: ex.javaClass.simpleName
                if (event.attempts + 1 >= maxAttempts) {
                    event.markFailed(message)
                    logger.error("Auth outbox event {} failed permanently", event.id, ex)
                } else {
                    event.markRetry(message, nextAttemptAt(event.attempts + 1))
                    logger.warn("Auth outbox event {} publish failed; will retry", event.id, ex)
                }
            }
        }
    }

    private fun nextAttemptAt(attempt: Int): Long {
        val delayMs = minOf(60_000L, 1_000L * (1L shl attempt.coerceAtMost(6)))
        return System.currentTimeMillis() + delayMs
    }
}
