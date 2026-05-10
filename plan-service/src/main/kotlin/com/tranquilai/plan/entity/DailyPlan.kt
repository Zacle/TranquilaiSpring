package com.tranquilai.plan.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "daily_plans")
class DailyPlan(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    /** Epoch milliseconds for the start of the plan day (midnight UTC) */
    @Column(name = "plan_date", nullable = false)
    val planDate: Long,

    @Column(nullable = false, columnDefinition = "TEXT")
    val greeting: String,

    @Column(name = "motivational_message", nullable = false, columnDefinition = "TEXT")
    val motivationalMessage: String,

    @Column(name = "total_duration_minutes", nullable = false)
    var totalDurationMinutes: Int = 0,

    @Column(name = "completed_activities", nullable = false)
    var completedActivities: Int = 0,

    @Column(name = "is_fully_completed", nullable = false)
    var isFullyCompleted: Boolean = false,

    @Column(name = "ai_generated_at", nullable = false)
    val aiGeneratedAt: Long = System.currentTimeMillis(),

    @Column(name = "created_at", nullable = false)
    val createdAt: Long = System.currentTimeMillis(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = System.currentTimeMillis(),

    @OneToMany(mappedBy = "plan", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    val activities: MutableList<PlanActivity> = mutableListOf(),
)
