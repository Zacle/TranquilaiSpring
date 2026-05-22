package com.tranquilai.activity.entity

import jakarta.persistence.*
import org.springframework.data.domain.Persistable
import java.util.UUID

@Entity
@Table(name = "mood_entries")
class MoodEntry(
    @Id
    @JvmField
    final val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "mood_score", nullable = false)
    var moodScore: Int,

    @Column(name = "mood_label")
    var moodLabel: String? = null,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

    /** Comma-separated list of contributing factors */
    var factors: String? = null,

    /** Comma-separated list of selected emotions */
    @Column(name = "emotions", columnDefinition = "TEXT")
    var emotions: String? = null,

    @Column(name = "ai_insight", columnDefinition = "TEXT")
    var aiInsight: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Long = System.currentTimeMillis(),
) : Persistable<UUID> {
    override fun getId(): UUID = id

    @Transient private var newEntity: Boolean = true
    override fun isNew(): Boolean = newEntity
    @PostLoad @PostPersist private fun markNotNew() { newEntity = false }
}
