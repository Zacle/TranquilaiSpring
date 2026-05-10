package com.tranquilai.content.entity

import jakarta.persistence.*

@Entity
@Table(name = "meditation_audio")
class MeditationAudio(
    @Id
    @Column(name = "topic_id", nullable = false, length = 100)
    val topicId: String,

    @Column(name = "audio_url", nullable = false, columnDefinition = "TEXT")
    val audioUrl: String,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Long = System.currentTimeMillis(),
)
