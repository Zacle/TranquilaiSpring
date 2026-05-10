package com.tranquilai.content.service

import com.tranquilai.content.entity.AmbientSoundAudio
import com.tranquilai.content.entity.MeditationAudio
import com.tranquilai.content.repository.AmbientSoundAudioRepository
import com.tranquilai.content.repository.MeditationAudioRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class AudioServiceTest {

    private val meditationAudioRepo: MeditationAudioRepository = mock(MeditationAudioRepository::class.java)
    private val ambientSoundAudioRepo: AmbientSoundAudioRepository = mock(AmbientSoundAudioRepository::class.java)
    private val service = AudioService(meditationAudioRepo, ambientSoundAudioRepo)

    @Test
    fun `getAllAudioUrls returns meditation and ambient audio keyed by content ids`() {
        `when`(meditationAudioRepo.findAll()).thenReturn(
            listOf(
                MeditationAudio(topicId = "mindfulness_calm_focus", audioUrl = "https://cdn/meditation/calm.mp3"),
                MeditationAudio(topicId = "sleep_deep_rest", audioUrl = "https://cdn/meditation/sleep.mp3"),
            ),
        )
        `when`(ambientSoundAudioRepo.findAll()).thenReturn(
            listOf(
                AmbientSoundAudio(soundId = "nature_rain", audioUrl = "https://cdn/ambient/rain.mp3"),
            ),
        )

        val response = service.getAllAudioUrls()

        assertEquals(
            mapOf(
                "mindfulness_calm_focus" to "https://cdn/meditation/calm.mp3",
                "sleep_deep_rest" to "https://cdn/meditation/sleep.mp3",
            ),
            response.meditationAudio,
        )
        assertEquals(mapOf("nature_rain" to "https://cdn/ambient/rain.mp3"), response.ambientAudio)
    }
}
