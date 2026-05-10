package com.tranquilai.activity.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "meditation_sessions")
class MeditationSession(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "topic_id", nullable = false)
    val topicId: String,

    @Column(name = "meditation_title", nullable = false)
    val meditationTitle: String,

    @Column(name = "duration_seconds", nullable = false)
    val durationSeconds: Int,

    @Column(name = "actual_duration_seconds", nullable = false)
    val actualDurationSeconds: Int,

    @Column(name = "completed_at", nullable = false)
    val completedAt: Long,

    @Column(name = "feeling_rating")
    val feelingRating: Int? = null,

    /** Comma-separated ambient sound IDs used during the session */
    @Column(name = "sounds_used")
    val soundsUsed: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Long = System.currentTimeMillis(),
)
