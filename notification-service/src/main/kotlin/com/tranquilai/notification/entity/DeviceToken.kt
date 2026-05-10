package com.tranquilai.notification.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "device_tokens")
class DeviceToken(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    var token: String,

    @Column(name = "device_name")
    var deviceName: String? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: Long = System.currentTimeMillis(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = System.currentTimeMillis(),
)
