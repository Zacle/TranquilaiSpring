package com.tranquilai.progress.exception

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class GlobalExceptionHandlerTest {

    @Test
    fun `handleGeneral returns sanitized internal error`() {
        val response = GlobalExceptionHandler().handleGeneral(IllegalStateException("secret detail"))

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("INTERNAL_ERROR", response.body?.error)
        assertEquals("An unexpected error occurred", response.body?.message)
        assertEquals("INTERNAL_ERROR", response.body?.errorCode)
    }
}
