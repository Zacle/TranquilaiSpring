package com.tranquilai.activity.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "affirmation_views")
class AffirmationView(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "affirmation_id", nullable = false)
    val affirmationId: String,

    @Column(name = "viewed_at", nullable = false)
    val viewedAt: Long = System.currentTimeMillis(),
)
