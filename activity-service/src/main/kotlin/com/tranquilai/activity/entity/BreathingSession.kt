package com.tranquilai.activity.entity

import jakarta.persistence.*
import org.springframework.data.domain.Persistable
import java.util.UUID

@Entity
@Table(name = "breathing_sessions")
class BreathingSession(
    @Id
    @JvmField
    final val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "exercise_id", nullable = false)
    val exerciseId: String,

    @Column(name = "exercise_title", nullable = false)
    val exerciseTitle: String,

    @Column(name = "selected_duration_seconds", nullable = false)
    val selectedDurationSeconds: Int,

    @Column(name = "actual_duration_seconds", nullable = false)
    val actualDurationSeconds: Int,

    @Column(name = "completed_cycles", nullable = false)
    val completedCycles: Int,

    @Column(name = "completed_at", nullable = false)
    val completedAt: Long,

    @Column(name = "feeling_rating")
    val feelingRating: Int? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Long = System.currentTimeMillis(),
) : Persistable<UUID> {
    override fun getId(): UUID = id

    @Transient private var newEntity: Boolean = true
    override fun isNew(): Boolean = newEntity
    @PostLoad @PostPersist private fun markNotNew() { newEntity = false }
}
