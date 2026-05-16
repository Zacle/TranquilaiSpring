package com.tranquilai.progress.entity

import jakarta.persistence.*
import org.springframework.data.domain.Persistable
import java.util.UUID

@Entity
@Table(name = "badges")
class Badge(
    @Id
    @JvmField
    final val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "badge_type", nullable = false)
    val badgeType: String,

    @Column(name = "badge_name", nullable = false)
    val badgeName: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column(name = "awarded_at", nullable = false)
    val awardedAt: Long = System.currentTimeMillis(),
) : Persistable<UUID> {
    override fun getId(): UUID = id

    @Transient private var newEntity: Boolean = true
    override fun isNew(): Boolean = newEntity
    @PostLoad @PostPersist private fun markNotNew() { newEntity = false }
}
