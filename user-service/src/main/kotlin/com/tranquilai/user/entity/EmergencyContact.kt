package com.tranquilai.user.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "emergency_contacts")
class EmergencyContact(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(nullable = false)
    var name: String,

    @Column(name = "phone_number")
    var phoneNumber: String? = null,

    var email: String? = null,

    @Column(nullable = false)
    var relationship: String,

    @Column(name = "is_primary", nullable = false)
    var isPrimary: Boolean = false,

    @Column(name = "created_at", nullable = false)
    val createdAt: Long = System.currentTimeMillis(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = System.currentTimeMillis(),
)
