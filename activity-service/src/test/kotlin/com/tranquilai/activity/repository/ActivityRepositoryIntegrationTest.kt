package com.tranquilai.activity.repository

import com.tranquilai.activity.entity.AffirmationView
import com.tranquilai.activity.entity.BreathingSession
import com.tranquilai.activity.entity.JournalEntry
import com.tranquilai.activity.entity.MeditationSession
import com.tranquilai.activity.entity.MoodEntry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.domain.PageRequest
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
class ActivityRepositoryIntegrationTest @Autowired constructor(
    private val moodRepository: MoodEntryRepository,
    private val journalRepository: JournalEntryRepository,
    private val breathingRepository: BreathingSessionRepository,
    private val meditationRepository: MeditationSessionRepository,
    private val affirmationViewRepository: AffirmationViewRepository,
) {

    @Test
    fun `mood repository supports paging date range and today lookup`() {
        val userId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val older = moodRepository.save(MoodEntry(userId = userId, moodScore = 4, createdAt = 100))
        val newer = moodRepository.save(MoodEntry(userId = userId, moodScore = 8, createdAt = 200))
        moodRepository.save(MoodEntry(userId = otherUserId, moodScore = 10, createdAt = 300))

        val page = moodRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 10))
        val range = moodRepository.findByUserIdAndDateRange(userId, 50, 250)

        assertEquals(listOf(newer.id, older.id), page.content.map { it.id })
        assertEquals(listOf(newer.id, older.id), range.map { it.id })
        assertEquals(2, moodRepository.countByUserIdAndCreatedAtBetween(userId, 50, 250))
        assertEquals(newer.id, moodRepository.findFirstByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, 50, 250)?.id)
    }

    @Test
    fun `journal repository supports category favorites and activity counts`() {
        val userId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val reflection = journalRepository.save(journal(userId, "reflection", true, 100))
        val gratitude = journalRepository.save(journal(userId, "gratitude", false, 200))
        journalRepository.save(journal(otherUserId, "reflection", true, 300))

        assertEquals(listOf(gratitude.id, reflection.id), journalRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 10)).content.map { it.id })
        assertEquals(listOf(reflection.id), journalRepository.findByUserIdAndIsFavoriteTrueOrderByCreatedAtDesc(userId).map { it.id })
        assertEquals(listOf(gratitude.id), journalRepository.findByUserIdAndCategoryOrderByCreatedAtDesc(userId, "gratitude", PageRequest.of(0, 10)).content.map { it.id })
        assertEquals(2, journalRepository.countByUserIdAndCreatedAtBetween(userId, 50, 250))
    }

    @Test
    fun `session repositories support history and counts`() {
        val userId = UUID.randomUUID()
        val olderBreathing = breathingRepository.save(breathing(userId, 100))
        val newerBreathing = breathingRepository.save(breathing(userId, 200))
        val olderMeditation = meditationRepository.save(meditation(userId, 100))
        val newerMeditation = meditationRepository.save(meditation(userId, 200))
        breathingRepository.save(breathing(UUID.randomUUID(), 300))
        meditationRepository.save(meditation(UUID.randomUUID(), 300))

        assertEquals(listOf(newerBreathing.id, olderBreathing.id), breathingRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, 50, 250).map { it.id })
        assertEquals(2, breathingRepository.countByUserId(userId))
        assertEquals(2, breathingRepository.countByUserIdAndCreatedAtBetween(userId, 50, 250))
        assertEquals(listOf(newerMeditation.id, olderMeditation.id), meditationRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, 50, 250).map { it.id })
        assertEquals(2, meditationRepository.countByUserId(userId))
        assertEquals(2, meditationRepository.countByUserIdAndCreatedAtBetween(userId, 50, 250))
    }

    @Test
    fun `affirmation view repository persists views`() {
        val view = affirmationViewRepository.save(AffirmationView(userId = UUID.randomUUID(), affirmationId = "self_worth"))

        assertTrue(affirmationViewRepository.findById(view.id).isPresent)
    }

    private fun journal(userId: UUID, category: String, isFavorite: Boolean, createdAt: Long) =
        JournalEntry(
            userId = userId,
            category = category,
            content = "Entry",
            isFavorite = isFavorite,
            createdAt = createdAt,
            updatedAt = createdAt,
        )

    private fun breathing(userId: UUID, createdAt: Long) =
        BreathingSession(
            userId = userId,
            exerciseId = "box_breathing",
            exerciseTitle = "Box Breathing",
            selectedDurationSeconds = 60,
            actualDurationSeconds = 60,
            completedCycles = 4,
            completedAt = createdAt,
            createdAt = createdAt,
        )

    private fun meditation(userId: UUID, createdAt: Long) =
        MeditationSession(
            userId = userId,
            topicId = "mindfulness",
            meditationTitle = "Mindfulness",
            durationSeconds = 60,
            actualDurationSeconds = 60,
            completedAt = createdAt,
            createdAt = createdAt,
        )
}
