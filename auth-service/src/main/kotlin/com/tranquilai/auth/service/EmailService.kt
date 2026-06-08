package com.tranquilai.auth.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    private val templateEngine: TemplateEngine,
    @param:Value("\${app.email.from}") private val fromEmail: String,
    @param:Value("\${app.email.from-name}") private val fromName: String,
    @param:Value("\${app.email.reply-to}") private val replyToEmail: String,
) {
    fun sendVerificationEmail(toEmail: String, firstName: String, code: String) {
        val context = Context().apply {
            setVariable("firstName", firstName)
            setVariable("code", code)
        }
        val html = templateEngine.process("email/verification", context)
        sendEmail(toEmail, "Verify Your TranquilAI Account", html)
    }

    fun sendPasswordResetEmail(toEmail: String, firstName: String, code: String) {
        val context = Context().apply {
            setVariable("firstName", firstName)
            setVariable("code", code)
        }
        val html = templateEngine.process("email/reset-password", context)
        sendEmail(toEmail, "Reset Your TranquilAI Password", html)
    }

    private fun sendEmail(to: String, subject: String, htmlContent: String) {
        val message = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true, "UTF-8")
        helper.setFrom("$fromName <$fromEmail>")
        helper.setReplyTo(replyToEmail)
        helper.setTo(to)
        helper.setSubject(subject)
        helper.setText(htmlContent, true)
        mailSender.send(message)
    }
}
