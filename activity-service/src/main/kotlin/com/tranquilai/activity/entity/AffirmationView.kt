package com.tranquilai.activity.entity

import jakarta.persistence.*
import org.springframework.data.domain.Persistable
import java.util.UUID

@Entity
@Table(name = "affirmation_views")
class AffirmationView(
    @Id
    @JvmField
    final val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "affirmation_id", nullable = false)
    val affirmationId: String,

    @Column(name = "viewed_at", nullable = false)
    val viewedAt: Long = System.currentTimeMillis(),
) : Persistable<UUID> {
    override fun getId(): UUID = id

    @Transient private var newEntity: Boolean = true
    override fun isNew(): Boolean = newEntity
    @PostLoad @PostPersist private fun markNotNew() { newEntity = false }
}
