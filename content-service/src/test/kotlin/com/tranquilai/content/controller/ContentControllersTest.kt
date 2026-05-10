package com.tranquilai.content.controller

import com.tranquilai.content.entity.ContentType
import com.tranquilai.content.security.GatewayUser
import com.tranquilai.content.service.AudioService
import com.tranquilai.content.service.AudioUrlsResponse
import com.tranquilai.content.service.FavoriteService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

class ContentControllersTest {

    private val favoriteService: FavoriteService = mock(FavoriteService::class.java)
    private val audioService: AudioService = mock(AudioService::class.java)
    private val user = GatewayUser(UUID.randomUUID(), "user@example.com", "USER")

    @Test
    fun `audio controller returns all audio urls`() {
        val response = AudioUrlsResponse(
            meditationAudio = mapOf("mindfulness_calm_focus" to "https://cdn/meditation/calm.mp3"),
            ambientAudio = mapOf("nature_rain" to "https://cdn/ambient/rain.mp3"),
        )
        `when`(audioService.getAllAudioUrls()).thenReturn(response)

        val result = ContentAudioController(audioService).getAudioUrls()

        assertEquals(response, result.body)
    }

    @Test
    fun `meditation controller uses meditation content type`() {
        `when`(favoriteService.getFavorites(user.id, ContentType.MEDITATION)).thenReturn(listOf("mindfulness"))
        `when`(favoriteService.toggle(user.id, ContentType.MEDITATION, "mindfulness")).thenReturn(mapOf("isFavorite" to true))
        val controller = MeditationTopicController(favoriteService)

        assertEquals(listOf("mindfulness"), controller.favorites(user).body)
        assertEquals(mapOf("isFavorite" to true), controller.toggleFavorite(user, "mindfulness").body)
        verify(favoriteService).getFavorites(user.id, ContentType.MEDITATION)
        verify(favoriteService).toggle(user.id, ContentType.MEDITATION, "mindfulness")
    }

    @Test
    fun `breathing controller uses breathing content type`() {
        val controller = BreathingExerciseController(favoriteService)

        controller.favorites(user)
        controller.toggleFavorite(user, "box_breathing")

        verify(favoriteService).getFavorites(user.id, ContentType.BREATHING)
        verify(favoriteService).toggle(user.id, ContentType.BREATHING, "box_breathing")
    }

    @Test
    fun `affirmation controller uses affirmation content type`() {
        val controller = AffirmationController(favoriteService)

        controller.favorites(user)
        controller.toggleFavorite(user, "self_worth")

        verify(favoriteService).getFavorites(user.id, ContentType.AFFIRMATION)
        verify(favoriteService).toggle(user.id, ContentType.AFFIRMATION, "self_worth")
    }

    @Test
    fun `journal prompt controller uses journal prompt content type`() {
        val controller = JournalPromptController(favoriteService)

        controller.favorites(user)
        controller.toggleFavorite(user, "gratitude")

        verify(favoriteService).getFavorites(user.id, ContentType.JOURNAL_PROMPT)
        verify(favoriteService).toggle(user.id, ContentType.JOURNAL_PROMPT, "gratitude")
    }
}
