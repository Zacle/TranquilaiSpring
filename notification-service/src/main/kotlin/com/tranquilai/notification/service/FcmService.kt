package com.tranquilai.notification.service

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.Notification
import com.tranquilai.notification.entity.NotificationLog
import com.tranquilai.notification.repository.DeviceTokenRepository
import com.tranquilai.notification.repository.NotificationLogRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class PushPayload(
    val title: String,
    val body: String,
    val notificationType: String,
    val data: Map<String, String> = emptyMap(),
)

data class PushResult(val sent: Int, val failed: Int, val skipped: Int)

@Service
class FcmService(
    private val firebaseMessaging: FirebaseMessaging,
    private val deviceTokenRepo: DeviceTokenRepository,
    private val logRepo: NotificationLogRepository,
) {
    private val logger = LoggerFactory.getLogger(FcmService::class.java)

    /**
     * Sends a push notification to all active device tokens of the given user.
     * Tokens that are no longer registered in FCM are deactivated automatically.
     */
    @Transactional
    fun sendToUser(userId: UUID, payload: PushPayload): PushResult {
        val tokens = deviceTokenRepo.findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(userId)
        if (tokens.isEmpty()) return PushResult(sent = 0, failed = 0, skipped = 1)

        var sent = 0; var failed = 0
        for (deviceToken in tokens) {
            when (val result = sendToToken(deviceToken.token, payload)) {
                "OK" -> {
                    sent++
                    logRepo.save(notificationLog(userId, payload, "SENT", deviceToken.token))
                }
                "UNREGISTERED" -> {
                    // Token is stale — deactivate it so we don't keep sending to it
                    deviceToken.isActive = false
                    deviceToken.updatedAt = System.currentTimeMillis()
                    deviceTokenRepo.save(deviceToken)
                    logger.info("Deactivated stale FCM token for user $userId")
                    logRepo.save(notificationLog(userId, payload, "FAILED", deviceToken.token, "Token unregistered"))
                    failed++
                }
                else -> {
                    failed++
                    logRepo.save(notificationLog(userId, payload, "FAILED", deviceToken.token, result))
                }
            }
        }
        return PushResult(sent = sent, failed = failed, skipped = 0)
    }

    /** Returns "OK", "UNREGISTERED", or an error description string. */
    private fun sendToToken(token: String, payload: PushPayload): String {
        return try {
            val message = Message.builder()
                .setToken(token)
                .setNotification(
                    Notification.builder()
                        .setTitle(payload.title)
                        .setBody(payload.body)
                        .build()
                )
                .putAllData(payload.data + mapOf("type" to payload.notificationType))
                .build()

            firebaseMessaging.send(message)
            "OK"
        } catch (ex: FirebaseMessagingException) {
            if (ex.messagingErrorCode == MessagingErrorCode.UNREGISTERED) {
                "UNREGISTERED"
            } else {
                logger.error("FCM error sending to token: ${ex.messagingErrorCode} — ${ex.message}")
                ex.messagingErrorCode?.name ?: "FCM_ERROR"
            }
        }
    }

    private fun notificationLog(
        userId: UUID,
        payload: PushPayload,
        status: String,
        token: String,
        error: String? = null,
    ) = NotificationLog(
        userId = userId,
        title = payload.title,
        body = payload.body,
        notificationType = payload.notificationType,
        status = status,
        deviceToken = token,
        errorMessage = error,
    )
}
