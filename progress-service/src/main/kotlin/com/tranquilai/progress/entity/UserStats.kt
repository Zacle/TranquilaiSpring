package com.tranquilai.progress.entity

import jakarta.persistence.*
import org.springframework.data.domain.Persistable
import java.util.UUID

@Entity
@Table(name = "user_stats")
class UserStats(
    @Id
    @JvmField
    final val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false, unique = true)
    val userId: UUID,

    @Column(name = "total_sessions", nullable = false)
    var totalSessions: Int = 0,

    @Column(name = "total_minutes", nullable = false)
    var totalMinutes: Int = 0,

    @Column(name = "current_streak_days", nullable = false)
    var currentStreakDays: Int = 0,

    @Column(name = "longest_streak_days", nullable = false)
    var longestStreakDays: Int = 0,

    @Column(name = "mood_entries_count", nullable = false)
    var moodEntriesCount: Int = 0,

    @Column(name = "journal_entries_count", nullable = false)
    var journalEntriesCount: Int = 0,

    @Column(name = "plans_completed", nullable = false)
    var plansCompleted: Int = 0,

    @Column(name = "total_days_active", nullable = false)
    var totalDaysActive: Int = 0,

    @Column(name = "total_chat_sessions", nullable = false)
    var totalChatSessions: Int = 0,

    @Column(name = "last_active_date")
    var lastActiveDate: Long? = null,

    @Column(name = "streak_start_date")
    var streakStartDate: Long? = null,

    @Column(name = "average_mood_score")
    var averageMoodScore: Double? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Long = System.currentTimeMillis(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = System.currentTimeMillis(),
) : Persistable<UUID> {
    override fun getId(): UUID = id

    @Transient private var newEntity: Boolean = true
    override fun isNew(): Boolean = newEntity
    @PostLoad @PostPersist private fun markNotNew() { newEntity = false }
}
