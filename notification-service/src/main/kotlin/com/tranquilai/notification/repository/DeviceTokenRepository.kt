package com.tranquilai.notification.repository

import com.tranquilai.notification.entity.DeviceToken
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface DeviceTokenRepository : JpaRepository<DeviceToken, UUID> {
    fun findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(userId: UUID): List<DeviceToken>
    fun findByToken(token: String): Optional<DeviceToken>
    fun existsByUserIdAndToken(userId: UUID, token: String): Boolean
    fun deleteByUserId(userId: UUID)
}
