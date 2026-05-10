package com.tranquilai.auth.service

import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.mail.javamail.JavaMailSender
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import java.util.Properties

class EmailServiceTest {

    private val mailSender: JavaMailSender = mock(JavaMailSender::class.java)
    private val templateEngine: TemplateEngine = mock(TemplateEngine::class.java)
    private val service = EmailService(
        mailSender = mailSender,
        templateEngine = templateEngine,
        fromEmail = "noreply@tranquilai.cloud",
        fromName = "TranquilAI",
    )

    @Test
    fun `sendVerificationEmail renders verification template and sends html email`() {
        val message = MimeMessage(Session.getInstance(Properties()))
        `when`(mailSender.createMimeMessage()).thenReturn(message)
        `when`(templateEngine.process(org.mockito.ArgumentMatchers.eq("email/verification"), org.mockito.ArgumentMatchers.any(Context::class.java)))
            .thenReturn("<html>verification-code</html>")

        service.sendVerificationEmail("user@example.com", "Test", "123456")

        val contextCaptor = ArgumentCaptor.forClass(Context::class.java)
        verify(templateEngine).process(org.mockito.ArgumentMatchers.eq("email/verification"), contextCaptor.capture())
        verify(mailSender).send(message)

        assertEquals("Test", contextCaptor.value.getVariable("firstName"))
        assertEquals("123456", contextCaptor.value.getVariable("code"))
        assertEquals("Verify Your TranquilAI Account", message.subject)
        assertEquals("user@example.com", message.getRecipients(Message.RecipientType.TO).single().toString())
        assertEquals("TranquilAI <noreply@tranquilai.cloud>", message.from.single().toString())
    }

    @Test
    fun `sendPasswordResetEmail renders reset template and sends html email`() {
        val message = MimeMessage(Session.getInstance(Properties()))
        `when`(mailSender.createMimeMessage()).thenReturn(message)
        `when`(templateEngine.process(org.mockito.ArgumentMatchers.eq("email/reset-password"), org.mockito.ArgumentMatchers.any(Context::class.java)))
            .thenReturn("<html>reset-code</html>")

        service.sendPasswordResetEmail("user@example.com", "Test", "654321")

        val contextCaptor = ArgumentCaptor.forClass(Context::class.java)
        verify(templateEngine).process(org.mockito.ArgumentMatchers.eq("email/reset-password"), contextCaptor.capture())
        verify(mailSender).send(message)

        assertEquals("Test", contextCaptor.value.getVariable("firstName"))
        assertEquals("654321", contextCaptor.value.getVariable("code"))
        assertEquals("Reset Your TranquilAI Password", message.subject)
        assertEquals("user@example.com", message.getRecipients(Message.RecipientType.TO).single().toString())
        assertEquals("TranquilAI <noreply@tranquilai.cloud>", message.from.single().toString())
    }
}
