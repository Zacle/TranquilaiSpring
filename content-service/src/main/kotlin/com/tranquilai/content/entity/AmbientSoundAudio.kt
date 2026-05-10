package com.tranquilai.content.entity

import jakarta.persistence.*

@Entity
@Table(name = "ambient_sound_audio")
class AmbientSoundAudio(
    @Id
    @Column(name = "sound_id", nullable = false, length = 100)
    val soundId: String,

    @Column(name = "audio_url", nullable = false, columnDefinition = "TEXT")
    val audioUrl: String,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Long = System.currentTimeMillis(),
)
