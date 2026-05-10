package com.tranquilai.progress.repository

import com.tranquilai.progress.entity.Badge
import com.tranquilai.progress.entity.UserStats
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.dao.DataIntegrityViolationException
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
class ProgressRepositoryIntegrationTest @Autowired constructor(
    private val userStatsRepository: UserStatsRepository,
    private val badgeRepository: BadgeRepository,
) {

    @Test
    fun `user stats repository finds by user id and enforces uniqueness`() {
        val userId = UUID.randomUUID()
        userStatsRepository.saveAndFlush(UserStats(userId = userId, totalSessions = 4))

        val found = userStatsRepository.findByUserId(userId)

        assertTrue(found.isPresent)
        assertEquals(4, found.get().totalSessions)
        assertTrue(userStatsRepository.findByUserId(UUID.randomUUID()).isEmpty)

        userStatsRepository.save(UserStats(userId = userId))
        org.junit.jupiter.api.assertThrows<DataIntegrityViolationException> {
            userStatsRepository.flush()
        }
    }

    @Test
    fun `badge repository orders badges by award time and checks existence by type`() {
        val userId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        badgeRepository.save(Badge(userId = userId, badgeType = "FIRST", badgeName = "First", awardedAt = 100))
        badgeRepository.save(Badge(userId = userId, badgeType = "SECOND", badgeName = "Second", awardedAt = 300))
        badgeRepository.save(Badge(userId = userId, badgeType = "THIRD", badgeName = "Third", awardedAt = 200))
        badgeRepository.save(Badge(userId = otherUserId, badgeType = "OTHER", badgeName = "Other", awardedAt = 400))

        val badges = badgeRepository.findByUserIdOrderByAwardedAtDesc(userId)

        assertEquals(listOf("SECOND", "THIRD", "FIRST"), badges.map { it.badgeType })
        assertTrue(badgeRepository.existsByUserIdAndBadgeType(userId, "SECOND"))
        assertFalse(badgeRepository.existsByUserIdAndBadgeType(userId, "MISSING"))
        assertEquals(1, badgeRepository.findByUserIdOrderByAwardedAtDesc(otherUserId).size)
    }
}
