package com.tranquilai.subscription.exception

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test
    fun `maps domain exceptions to expected statuses`() {
        assertEquals(HttpStatus.NOT_FOUND, handler.handleNotFound(ResourceNotFoundException("missing")).statusCode)
        assertEquals(HttpStatus.BAD_REQUEST, handler.handleSubscription(SubscriptionException("bad")).statusCode)
        assertEquals(HttpStatus.BAD_REQUEST, handler.handleWebhook(WebhookException("bad signature")).statusCode)
    }

    @Test
    fun `general handler hides internal detail`() {
        val response = handler.handleGeneral(IllegalStateException("secret"))

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("An unexpected error occurred", response.body?.message)
    }
}
