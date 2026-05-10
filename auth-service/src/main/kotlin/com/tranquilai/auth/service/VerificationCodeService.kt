package com.tranquilai.auth.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.pow

@Service
class VerificationCodeService(
    private val redisTemplate: StringRedisTemplate,
    @param:Value("\${app.verification.code-expiry-minutes}") private val expiryMinutes: Long,
    @param:Value("\${app.verification.code-length}") private val codeLength: Int,
) {
    companion object {
        private const val EMAIL_VERIFY_PREFIX = "email:verify:"
        private const val PASSWORD_RESET_PREFIX = "password:reset:"
    }

    fun generateAndStoreEmailVerificationCode(email: String): String {
        val code = generateCode()
        redisTemplate.opsForValue().set(
            "$EMAIL_VERIFY_PREFIX$email",
            code,
            Duration.ofMinutes(expiryMinutes),
        )
        return code
    }

    fun generateAndStorePasswordResetCode(email: String): String {
        val code = generateCode()
        redisTemplate.opsForValue().set(
            "$PASSWORD_RESET_PREFIX$email",
            code,
            Duration.ofMinutes(expiryMinutes),
        )
        return code
    }

    fun verifyEmailCode(email: String, code: String): Boolean {
        val stored = redisTemplate.opsForValue().get("$EMAIL_VERIFY_PREFIX$email")
        return stored == code
    }

    fun verifyPasswordResetCode(email: String, code: String): Boolean {
        val stored = redisTemplate.opsForValue().get("$PASSWORD_RESET_PREFIX$email")
        return stored == code
    }

    fun deleteEmailVerificationCode(email: String) {
        redisTemplate.delete("$EMAIL_VERIFY_PREFIX$email")
    }

    fun deletePasswordResetCode(email: String) {
        redisTemplate.delete("$PASSWORD_RESET_PREFIX$email")
    }

    private fun generateCode(): String {
        val bound = 10.0.pow(codeLength.toDouble()).toInt()
        val start = 10.0.pow((codeLength - 1).toDouble()).toInt()
        return ThreadLocalRandom.current().nextInt(start, bound).toString()
    }
}
