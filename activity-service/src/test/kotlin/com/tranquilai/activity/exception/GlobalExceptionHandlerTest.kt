package com.tranquilai.activity.exception

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test
    fun `handleNotFound returns not found response`() {
        val response = handler.handleNotFound(ResourceNotFoundException("missing activity"))

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals(404, response.body?.status)
        assertEquals("NOT_FOUND", response.body?.errorCode)
        assertEquals("missing activity", response.body?.message)
    }

    @Test
    fun `handleGeneral hides internal details`() {
        val response = handler.handleGeneral(IllegalStateException("internal failure"))

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals(500, response.body?.status)
        assertEquals("INTERNAL_ERROR", response.body?.errorCode)
        assertEquals("An unexpected error occurred", response.body?.message)
    }
}
