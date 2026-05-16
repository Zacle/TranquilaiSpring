package com.tranquilai.user.entity

import jakarta.persistence.*
import org.springframework.data.domain.Persistable
import java.util.UUID

@Entity
@Table(name = "emergency_contacts")
class EmergencyContact(
    @Id
    @JvmField
    final val id: UUID = UUID.randomUUID(),

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
) : Persistable<UUID> {
    override fun getId(): UUID = id

    @Transient
    private var newEntity: Boolean = true

    override fun isNew(): Boolean = newEntity

    @PostLoad
    @PostPersist
    private fun markNotNew() {
        newEntity = false
    }
}
