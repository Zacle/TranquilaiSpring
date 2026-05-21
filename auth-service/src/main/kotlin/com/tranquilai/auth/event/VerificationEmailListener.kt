package com.tranquilai.auth.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.tranquilai.auth.service.EmailService
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

@Component
class VerificationEmailListener(
    private val objectMapper: ObjectMapper,
    private val emailService: EmailService,
) {
    private val logger = LoggerFactory.getLogger(VerificationEmailListener::class.java)

    @RabbitListener(queues = ["\${app.events.verification-email-queue}"])
    fun handle(message: String) {
        val event = objectMapper.readValue(message, VerificationEmailRequestedEvent::class.java)
        emailService.sendVerificationEmail(event.email, event.firstName, event.code)
        logger.info("Sent verification email for auth user {}", event.userId)
    }
}
