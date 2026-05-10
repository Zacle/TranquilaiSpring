package com.tranquilai.plan.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "plan_activities")
class PlanActivity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    val plan: DailyPlan,

    /** MOOD_TRACKING | CHAT_WITH_AI | JOURNALING | BREATHING_EXERCISE | MEDITATION | AFFIRMATION */
    @Column(name = "activity_type", nullable = false)
    val activityType: String,

    @Column(name = "content_id")
    val contentId: UUID? = null,

    @Column(nullable = false)
    val title: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    /** AI-generated contextual prompt for journaling, affirmation, or reflection activities */
    @Column(columnDefinition = "TEXT")
    val prompt: String? = null,

    @Column(name = "duration_minutes", nullable = false)
    val durationMinutes: Int,

    @Column(name = "sort_order", nullable = false)
    val sortOrder: Int = 0,

    @Column(name = "is_completed", nullable = false)
    var isCompleted: Boolean = false,

    @Column(name = "completed_at")
    var completedAt: Long? = null,

    /** User's satisfaction rating after completing the activity (1-5) */
    @Column(name = "completion_rating")
    var completionRating: Int? = null,

    @Column(name = "completion_notes", columnDefinition = "TEXT")
    var completionNotes: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Long = System.currentTimeMillis(),
)
