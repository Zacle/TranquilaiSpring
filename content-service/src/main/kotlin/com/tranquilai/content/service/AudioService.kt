package com.tranquilai.content.service

import com.tranquilai.content.repository.AmbientSoundAudioRepository
import com.tranquilai.content.repository.MeditationAudioRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AudioService(
    private val meditationAudioRepo: MeditationAudioRepository,
    private val ambientSoundAudioRepo: AmbientSoundAudioRepository,
) {
    fun getAllAudioUrls(): AudioUrlsResponse {
        val meditationAudio = meditationAudioRepo.findAll().associate { it.topicId to it.audioUrl }
        val ambientAudio = ambientSoundAudioRepo.findAll().associate { it.soundId to it.audioUrl }
        return AudioUrlsResponse(meditationAudio = meditationAudio, ambientAudio = ambientAudio)
    }
}

data class AudioUrlsResponse(
    val meditationAudio: Map<String, String>,
    val ambientAudio: Map<String, String>,
)
