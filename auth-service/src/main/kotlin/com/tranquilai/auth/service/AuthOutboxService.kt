package com.tranquilai.auth.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tranquilai.auth.entity.OutboxEvent
import com.tranquilai.auth.entity.User
import com.tranquilai.auth.event.AuthEventNames
import com.tranquilai.auth.event.OutboxEventQueuedEvent
import com.tranquilai.auth.event.UserVerifiedEvent
import com.tranquilai.auth.event.VerificationEmailRequestedEvent
import com.tranquilai.auth.repository.OutboxEventRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class AuthOutboxService(
    private val repository: OutboxEventRepository,
    private val objectMapper: ObjectMapper,
    private val applicationEventPublisher: ApplicationEventPublisher,
    @param:Value("\${app.events.verification-email-routing-key}") private val verificationEmailRoutingKey: String,
    @param:Value("\${app.events.user-verified-routing-key}") private val userVerifiedRoutingKey: String,
) {
    fun enqueueVerificationEmail(user: User, code: String) {
        enqueue(
            aggregateId = user.id.toString(),
            eventType = AuthEventNames.VERIFICATION_EMAIL_REQUESTED,
            routingKey = verificationEmailRoutingKey,
            payload = VerificationEmailRequestedEvent(
                userId = user.id,
                email = user.email,
                firstName = user.firstName,
                code = code,
            ),
        )
    }

    fun enqueueUserVerified(user: User) {
        enqueue(
            aggregateId = user.id.toString(),
            eventType = AuthEventNames.USER_VERIFIED,
            routingKey = userVerifiedRoutingKey,
            payload = UserVerifiedEvent(
                userId = user.id,
                email = user.email,
                username = user.username,
                firstName = user.firstName,
                lastName = user.lastName,
            ),
        )
    }

    private fun enqueue(
        aggregateId: String,
        eventType: String,
        routingKey: String,
        payload: Any,
    ) {
        val event = repository.save(
            OutboxEvent(
                aggregateType = "auth-user",
                aggregateId = aggregateId,
                eventType = eventType,
                routingKey = routingKey,
                payload = objectMapper.writeValueAsString(payload),
            ),
        )
        applicationEventPublisher.publishEvent(OutboxEventQueuedEvent(event.id))
    }
}
