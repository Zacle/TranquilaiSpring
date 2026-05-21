package com.tranquilai.user.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.tranquilai.user.dto.request.CreateUserRequest
import com.tranquilai.user.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

@Component
class UserVerifiedListener(
    private val objectMapper: ObjectMapper,
    private val userService: UserService,
) {
    private val logger = LoggerFactory.getLogger(UserVerifiedListener::class.java)

    @RabbitListener(queues = ["\${app.events.user-verified-queue}"])
    fun handle(message: String) {
        val event = objectMapper.readValue(message, UserVerifiedEvent::class.java)
        userService.createUser(
            CreateUserRequest(
                id = event.userId,
                email = event.email,
                username = event.username,
                firstName = event.firstName,
                lastName = event.lastName,
            ),
        )
        logger.info("Synced verified auth user {} into user-service", event.userId)
    }
}
