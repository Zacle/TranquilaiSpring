package com.tranquilai.subscription.entity

import jakarta.persistence.*
import org.springframework.data.domain.Persistable
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "entitlements")
class EntitlementGrant(
    @Id
    @JvmField
    final val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "feature", nullable = false)
    val feature: String,

    @Column(name = "granted", nullable = false)
    val granted: Boolean = true,

    @Column(name = "source")
    val source: String? = null,

    @Column(name = "expires_at")
    val expiresAt: Instant? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
) : Persistable<UUID> {
    override fun getId(): UUID = id

    @Transient private var newEntity: Boolean = true
    override fun isNew(): Boolean = newEntity
    @PostLoad @PostPersist private fun markNotNew() { newEntity = false }
}
