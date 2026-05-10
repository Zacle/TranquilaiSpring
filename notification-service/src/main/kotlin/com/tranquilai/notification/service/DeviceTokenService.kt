package com.tranquilai.notification.service

import com.tranquilai.notification.dto.request.RegisterDeviceTokenRequest
import com.tranquilai.notification.dto.response.DeviceTokenResponse
import com.tranquilai.notification.entity.DeviceToken
import com.tranquilai.notification.exception.ResourceNotFoundException
import com.tranquilai.notification.repository.DeviceTokenRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class DeviceTokenService(private val repo: DeviceTokenRepository) {

    /**
     * Registers or refreshes an FCM token for the current user.
     * If the token already exists (any user), it is re-assigned to the requesting user
     * and reactivated — this handles device handoffs and app reinstalls.
     */
    fun register(userId: UUID, request: RegisterDeviceTokenRequest): DeviceTokenResponse {
        val existing = repo.findByToken(request.token)
        return if (existing.isPresent) {
            val token = existing.get()
            token.isActive = true
            token.deviceName = request.deviceName ?: token.deviceName
            token.updatedAt = System.currentTimeMillis()
            repo.save(token).toResponse()
        } else {
            repo.save(
                DeviceToken(
                    userId = userId,
                    token = request.token,
                    deviceName = request.deviceName,
                )
            ).toResponse()
        }
    }

    /** Deactivates (soft-deletes) a device token on logout or uninstall. */
    fun deactivate(userId: UUID, token: String) {
        val deviceToken = repo.findByToken(token)
            .orElseThrow { ResourceNotFoundException("Device token not found") }
        if (deviceToken.userId != userId) throw ResourceNotFoundException("Device token not found")
        deviceToken.isActive = false
        deviceToken.updatedAt = System.currentTimeMillis()
        repo.save(deviceToken)
    }

    @Transactional(readOnly = true)
    fun listForUser(userId: UUID): List<DeviceTokenResponse> =
        repo.findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(userId).map { it.toResponse() }
}

fun DeviceToken.toResponse() = DeviceTokenResponse(
    id = id,
    userId = userId,
    deviceName = deviceName,
    isActive = isActive,
    createdAt = createdAt,
)
