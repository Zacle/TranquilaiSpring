package com.tranquilai.auth.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration

class VerificationCodeServiceTest {

    private val redisTemplate: StringRedisTemplate = mock(StringRedisTemplate::class.java)
    private val valueOperations: ValueOperations<String, String> = mock(ValueOperations::class.java) as ValueOperations<String, String>
    private val service = VerificationCodeService(
        redisTemplate = redisTemplate,
        expiryMinutes = 15,
        codeLength = 6,
    )

    init {
        `when`(redisTemplate.opsForValue()).thenReturn(valueOperations)
    }

    @Test
    fun `generateAndStoreEmailVerificationCode stores six digit code with ttl`() {
        val code = service.generateAndStoreEmailVerificationCode("user@example.com")

        val keyCaptor = ArgumentCaptor.forClass(String::class.java)
        val codeCaptor = ArgumentCaptor.forClass(String::class.java)
        val durationCaptor = ArgumentCaptor.forClass(Duration::class.java)
        verify(valueOperations).set(keyCaptor.capture(), codeCaptor.capture(), durationCaptor.capture())

        assertEquals("email:verify:user@example.com", keyCaptor.value)
        assertEquals(code, codeCaptor.value)
        assertEquals(Duration.ofMinutes(15), durationCaptor.value)
        assertTrue(code.matches(Regex("\\d{6}")))
    }

    @Test
    fun `generateAndStorePasswordResetCode stores six digit code with ttl`() {
        val code = service.generateAndStorePasswordResetCode("user@example.com")

        val keyCaptor = ArgumentCaptor.forClass(String::class.java)
        val codeCaptor = ArgumentCaptor.forClass(String::class.java)
        val durationCaptor = ArgumentCaptor.forClass(Duration::class.java)
        verify(valueOperations).set(keyCaptor.capture(), codeCaptor.capture(), durationCaptor.capture())

        assertEquals("password:reset:user@example.com", keyCaptor.value)
        assertEquals(code, codeCaptor.value)
        assertEquals(Duration.ofMinutes(15), durationCaptor.value)
        assertTrue(code.matches(Regex("\\d{6}")))
    }

    @Test
    fun `verifyEmailCode returns true only for matching stored code`() {
        `when`(valueOperations.get("email:verify:user@example.com")).thenReturn("123456")

        assertTrue(service.verifyEmailCode("user@example.com", "123456"))
        assertFalse(service.verifyEmailCode("user@example.com", "000000"))
    }

    @Test
    fun `verifyPasswordResetCode returns true only for matching stored code`() {
        `when`(valueOperations.get("password:reset:user@example.com")).thenReturn("123456")

        assertTrue(service.verifyPasswordResetCode("user@example.com", "123456"))
        assertFalse(service.verifyPasswordResetCode("user@example.com", "000000"))
    }

    @Test
    fun `deleteEmailVerificationCode deletes expected redis key`() {
        service.deleteEmailVerificationCode("user@example.com")

        verify(redisTemplate).delete("email:verify:user@example.com")
    }

    @Test
    fun `deletePasswordResetCode deletes expected redis key`() {
        service.deletePasswordResetCode("user@example.com")

        verify(redisTemplate).delete("password:reset:user@example.com")
    }
}
