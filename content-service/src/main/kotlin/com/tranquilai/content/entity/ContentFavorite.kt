package com.tranquilai.content.entity

import jakarta.persistence.*
import java.io.Serializable
import java.util.UUID

@Embeddable
data class ContentFavoriteId(
    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "content_type", nullable = false, length = 50)
    val contentType: String,

    @Column(name = "content_id", nullable = false, length = 100)
    val contentId: String,
) : Serializable

@Entity
@Table(name = "content_favorites")
class ContentFavorite(
    @EmbeddedId
    val id: ContentFavoriteId,

    @Column(name = "created_at", nullable = false)
    val createdAt: Long = System.currentTimeMillis(),
)

object ContentType {
    const val AFFIRMATION = "affirmation"
    const val BREATHING = "breathing"
    const val MEDITATION = "meditation"
    const val JOURNAL_PROMPT = "journal_prompt"
}
