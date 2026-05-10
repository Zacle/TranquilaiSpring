package com.tranquilai.content.repository

import com.tranquilai.content.entity.AmbientSoundAudio
import com.tranquilai.content.entity.ContentFavorite
import com.tranquilai.content.entity.ContentFavoriteId
import com.tranquilai.content.entity.MeditationAudio
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.TestPropertySource
import java.util.UUID

@DataJpaTest
@TestPropertySource(
    properties = [
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false",
    ],
)
class ContentRepositoryIntegrationTest @Autowired constructor(
    private val favoriteRepository: ContentFavoriteRepository,
    private val meditationAudioRepository: MeditationAudioRepository,
    private val ambientSoundAudioRepository: AmbientSoundAudioRepository,
) {

    @Test
    fun `favorite repository supports ordered lookup existence and custom delete`() {
        val userId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        favoriteRepository.save(ContentFavorite(ContentFavoriteId(userId, "meditation", "older"), createdAt = 100))
        favoriteRepository.save(ContentFavorite(ContentFavoriteId(userId, "meditation", "newer"), createdAt = 200))
        favoriteRepository.save(ContentFavorite(ContentFavoriteId(userId, "affirmation", "self_worth"), createdAt = 300))
        favoriteRepository.save(ContentFavorite(ContentFavoriteId(otherUserId, "meditation", "other"), createdAt = 400))

        val favorites = favoriteRepository.findByIdUserIdAndIdContentTypeOrderByCreatedAtDesc(userId, "meditation")

        assertEquals(listOf("newer", "older"), favorites.map { it.id.contentId })
        assertTrue(favoriteRepository.existsByIdUserIdAndIdContentTypeAndIdContentId(userId, "meditation", "newer"))
        assertFalse(favoriteRepository.existsByIdUserIdAndIdContentTypeAndIdContentId(userId, "meditation", "missing"))

        favoriteRepository.deleteByUserIdAndContentTypeAndContentId(userId, "meditation", "newer")
        favoriteRepository.flush()

        assertFalse(favoriteRepository.existsByIdUserIdAndIdContentTypeAndIdContentId(userId, "meditation", "newer"))
        assertEquals(1, favoriteRepository.findByIdUserIdAndIdContentTypeOrderByCreatedAtDesc(otherUserId, "meditation").size)
    }

    @Test
    fun `audio repositories persist meditation and ambient audio mappings`() {
        meditationAudioRepository.save(MeditationAudio("mindfulness_calm_focus", "https://cdn/meditation/calm.mp3"))
        ambientSoundAudioRepository.save(AmbientSoundAudio("nature_rain", "https://cdn/ambient/rain.mp3"))

        assertEquals(
            "https://cdn/meditation/calm.mp3",
            meditationAudioRepository.findById("mindfulness_calm_focus").get().audioUrl,
        )
        assertEquals(
            "https://cdn/ambient/rain.mp3",
            ambientSoundAudioRepository.findById("nature_rain").get().audioUrl,
        )
    }
}
