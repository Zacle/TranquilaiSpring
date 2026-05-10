package com.tranquilai.user.service

import com.tranquilai.user.dto.request.SaveMentalHealthProfileRequest
import com.tranquilai.user.dto.response.MentalHealthProfileResponse
import com.tranquilai.user.entity.MentalHealthProfile
import com.tranquilai.user.exception.UserNotFoundException
import com.tranquilai.user.repository.MentalHealthProfileRepository
import com.tranquilai.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class MentalHealthProfileService(
    private val profileRepository: MentalHealthProfileRepository,
    private val userRepository: UserRepository,
) {
    @Transactional(readOnly = true)
    fun getProfile(userId: UUID): MentalHealthProfileResponse? =
        profileRepository.findByUserId(userId).orElse(null)?.toResponse()

    /** Creates or fully replaces the mental health profile */
    fun saveProfile(userId: UUID, request: SaveMentalHealthProfileRequest): MentalHealthProfileResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException("User $userId not found") }

        val profile = profileRepository.findByUserId(userId).orElse(null)
            ?: MentalHealthProfile(user = user)

        // Questionnaire answers
        request.currentFeelingLevel?.let { profile.currentFeelingLevel = it }
        profile.stressCauses = request.stressCauses.joinToString(",").ifEmpty { profile.stressCauses }
        profile.currentConcerns = request.currentConcerns.joinToString(",").ifEmpty { profile.currentConcerns }
        profile.mentalProcessPreferences = request.mentalProcessPreferences.joinToString(",").ifEmpty { profile.mentalProcessPreferences }
        profile.personalGoals = request.personalGoals.joinToString(",").ifEmpty { profile.personalGoals }
        profile.identifiedTriggers = request.identifiedTriggers.joinToString(",").ifEmpty { profile.identifiedTriggers }

        // AI insights
        request.personalityAnalysis?.let { profile.personalityAnalysis = it }
        request.emotionalPatterns?.let { profile.emotionalPatterns = it }
        request.riskFactors?.let { profile.riskFactors = it }
        request.identifiedStrengths?.let { profile.identifiedStrengths = it }
        request.recommendedApproach?.let { profile.recommendedApproach = it }

        if (request.aiCopingStrategies.isNotEmpty()) {
            profile.aiCopingStrategies = request.aiCopingStrategies.joinToString(",")
        }
        if (request.aiFocusAreas.isNotEmpty()) {
            profile.aiFocusAreas = request.aiFocusAreas.joinToString(",")
        }

        // AI assessments
        request.urgencyLevel?.let { profile.urgencyLevel = it }
        request.supportIntensity?.let { profile.supportIntensity = it }
        request.communicationStyle?.let { profile.communicationStyle = it }

        // Baseline metrics
        request.baselineAnxietyLevel?.let { profile.baselineAnxietyLevel = it }
        request.baselineDepressionLevel?.let { profile.baselineDepressionLevel = it }
        request.baselineStressLevel?.let { profile.baselineStressLevel = it }
        request.baselineWellbeingLevel?.let { profile.baselineWellbeingLevel = it }
        request.baselineCopingAbility?.let { profile.baselineCopingAbility = it }

        // AI metadata
        if (request.aiAnalysisVersion != null) {
            profile.aiAnalysisVersion = request.aiAnalysisVersion
            profile.aiConfidenceScore = request.aiConfidenceScore
            profile.lastAiAnalysisAt = System.currentTimeMillis()
        }

        profile.updatedAt = System.currentTimeMillis()

        return profileRepository.save(profile).toResponse()
    }
}

private fun String?.toList(): List<String> =
    if (isNullOrBlank()) emptyList() else split(",").map { it.trim() }.filter { it.isNotEmpty() }

fun MentalHealthProfile.toResponse() = MentalHealthProfileResponse(
    id = id,
    userId = user.id,
    currentFeelingLevel = currentFeelingLevel,
    stressCauses = stressCauses.toList(),
    currentConcerns = currentConcerns.toList(),
    mentalProcessPreferences = mentalProcessPreferences.toList(),
    personalGoals = personalGoals.toList(),
    identifiedTriggers = identifiedTriggers.toList(),
    personalityAnalysis = personalityAnalysis,
    emotionalPatterns = emotionalPatterns,
    riskFactors = riskFactors,
    identifiedStrengths = identifiedStrengths,
    recommendedApproach = recommendedApproach,
    aiCopingStrategies = aiCopingStrategies.toList(),
    aiFocusAreas = aiFocusAreas.toList(),
    urgencyLevel = urgencyLevel,
    supportIntensity = supportIntensity,
    communicationStyle = communicationStyle,
    baselineAnxietyLevel = baselineAnxietyLevel,
    baselineDepressionLevel = baselineDepressionLevel,
    baselineStressLevel = baselineStressLevel,
    baselineWellbeingLevel = baselineWellbeingLevel,
    baselineCopingAbility = baselineCopingAbility,
    aiAnalysisVersion = aiAnalysisVersion,
    aiConfidenceScore = aiConfidenceScore,
    lastAiAnalysisAt = lastAiAnalysisAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
