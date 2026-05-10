package com.tranquilai.plan.exception

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test
    fun `handleNotFound returns not found error response`() {
        val response = handler.handleNotFound(ResourceNotFoundException("Plan not found"))

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals("NOT_FOUND", response.body?.error)
        assertEquals("Plan not found", response.body?.message)
        assertEquals("NOT_FOUND", response.body?.errorCode)
    }

    @Test
    fun `handleGeneral hides internal exception detail`() {
        val response = handler.handleGeneral(IllegalStateException("database password leaked"))

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("INTERNAL_ERROR", response.body?.error)
        assertEquals("An unexpected error occurred", response.body?.message)
    }
}
