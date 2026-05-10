package com.tranquilai.user.service

import com.tranquilai.user.dto.request.SaveMentalHealthProfileRequest
import com.tranquilai.user.entity.CommunicationStyle
import com.tranquilai.user.entity.MentalHealthProfile
import com.tranquilai.user.entity.SupportIntensity
import com.tranquilai.user.entity.UrgencyLevel
import com.tranquilai.user.entity.User
import com.tranquilai.user.exception.UserNotFoundException
import com.tranquilai.user.repository.MentalHealthProfileRepository
import com.tranquilai.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.Optional
import java.util.UUID

class MentalHealthProfileServiceTest {

    private val profileRepository: MentalHealthProfileRepository = mock(MentalHealthProfileRepository::class.java)
    private val userRepository: UserRepository = mock(UserRepository::class.java)
    private val service = MentalHealthProfileService(profileRepository, userRepository)

    @Test
    fun `getProfile returns null when no profile exists`() {
        val userId = UUID.randomUUID()
        `when`(profileRepository.findByUserId(userId)).thenReturn(Optional.empty())

        assertNull(service.getProfile(userId))
    }

    @Test
    fun `saveProfile creates profile with questionnaire ai and baseline fields`() {
        val user = testUser()
        `when`(userRepository.findById(user.id)).thenReturn(Optional.of(user))
        `when`(profileRepository.findByUserId(user.id)).thenReturn(Optional.empty())
        `when`(profileRepository.save(anyProfile())).thenAnswer { it.getArgument<MentalHealthProfile>(0) }

        val response = service.saveProfile(
            user.id,
            SaveMentalHealthProfileRequest(
                currentFeelingLevel = "overwhelmed",
                stressCauses = listOf("work", "sleep"),
                currentConcerns = listOf("focus"),
                mentalProcessPreferences = listOf("journaling"),
                personalGoals = listOf("calm"),
                identifiedTriggers = listOf("deadlines"),
                personalityAnalysis = "reflective",
                emotionalPatterns = "rumination",
                riskFactors = "burnout",
                identifiedStrengths = "self-aware",
                recommendedApproach = "gentle plan",
                aiCopingStrategies = listOf("breathing", "walk"),
                aiFocusAreas = listOf("sleep"),
                urgencyLevel = UrgencyLevel.MEDIUM,
                supportIntensity = SupportIntensity.MODERATE,
                communicationStyle = CommunicationStyle.EMPATHETIC,
                baselineAnxietyLevel = 7,
                baselineDepressionLevel = 4,
                baselineStressLevel = 8,
                baselineWellbeingLevel = 5,
                baselineCopingAbility = 6,
                aiAnalysisVersion = "v1",
                aiConfidenceScore = 0.82,
            ),
        )

        assertEquals(user.id, response.userId)
        assertEquals(listOf("work", "sleep"), response.stressCauses)
        assertEquals(listOf("breathing", "walk"), response.aiCopingStrategies)
        assertEquals(UrgencyLevel.MEDIUM, response.urgencyLevel)
        assertEquals(SupportIntensity.MODERATE, response.supportIntensity)
        assertEquals(CommunicationStyle.EMPATHETIC, response.communicationStyle)
        assertEquals(8, response.baselineStressLevel)
        assertEquals("v1", response.aiAnalysisVersion)
        assertEquals(0.82, response.aiConfidenceScore)
        assertNotNull(response.lastAiAnalysisAt)
        verify(profileRepository).save(anyProfile())
    }

    @Test
    fun `saveProfile updates existing profile without clearing omitted list fields`() {
        val user = testUser()
        val profile = MentalHealthProfile(user = user).apply {
            stressCauses = "work,sleep"
            aiCopingStrategies = "breathing"
        }
        `when`(userRepository.findById(user.id)).thenReturn(Optional.of(user))
        `when`(profileRepository.findByUserId(user.id)).thenReturn(Optional.of(profile))
        `when`(profileRepository.save(profile)).thenReturn(profile)

        val response = service.saveProfile(
            user.id,
            SaveMentalHealthProfileRequest(currentFeelingLevel = "better"),
        )

        assertEquals("better", response.currentFeelingLevel)
        assertEquals(listOf("work", "sleep"), response.stressCauses)
        assertEquals(listOf("breathing"), response.aiCopingStrategies)
    }

    @Test
    fun `saveProfile throws when user is missing`() {
        val userId = UUID.randomUUID()
        `when`(userRepository.findById(userId)).thenReturn(Optional.empty())

        assertThrows(UserNotFoundException::class.java) {
            service.saveProfile(userId, SaveMentalHealthProfileRequest())
        }
    }

    private fun testUser() = User(
        id = UUID.randomUUID(),
        email = "user@example.com",
        username = "user",
        firstName = "Test",
        lastName = "User",
    )

    @Suppress("UNCHECKED_CAST")
    private fun anyProfile(): MentalHealthProfile {
        any(MentalHealthProfile::class.java)
        return uninitialized()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> uninitialized(): T = null as T
}
