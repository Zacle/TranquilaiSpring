package com.tranquilai.auth.entity

import jakarta.persistence.*
import org.springframework.data.domain.Persistable
import java.util.UUID

@Entity
@Table(name = "refresh_tokens")
class RefreshToken(
    @Id
    @JvmField
    final val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false, unique = true, length = 2048)
    val token: String,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Long,

    @Column(name = "is_revoked", nullable = false)
    var isRevoked: Boolean = false,

    @Column(name = "created_at", nullable = false)
    val createdAt: Long = System.currentTimeMillis(),
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

    fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt
    fun isValid(): Boolean = !isRevoked && !isExpired()
}
