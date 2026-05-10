package com.tranquilai.progress.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "badges")
class Badge(
    @Id
    val id: UUID = UUID.randomUUID(),

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
)
