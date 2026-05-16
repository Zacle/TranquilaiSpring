package com.tranquilai.activity.entity

import jakarta.persistence.*
import org.springframework.data.domain.Persistable
import java.util.UUID

@Entity
@Table(name = "journal_entries")
class JournalEntry(
    @Id
    @JvmField
    final val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "prompt_id")
    val promptId: String? = null,

    @Column(name = "prompt_text", columnDefinition = "TEXT")
    val promptText: String? = null,

    val category: String? = null,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(name = "mood")
    var mood: Int? = null,

    @Column(name = "is_favorite", nullable = false)
    var isFavorite: Boolean = false,

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    var aiSummary: String? = null,

    /** Comma-separated AI-generated key themes */
    @Column(name = "ai_insights", columnDefinition = "TEXT")
    var aiInsights: String? = null,

    @Column(name = "emotional_tone")
    var emotionalTone: String? = null,

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
