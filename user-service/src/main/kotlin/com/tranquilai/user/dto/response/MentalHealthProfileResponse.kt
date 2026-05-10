package com.tranquilai.user.dto.response

import com.tranquilai.user.entity.CommunicationStyle
import com.tranquilai.user.entity.SupportIntensity
import com.tranquilai.user.entity.UrgencyLevel
import java.util.UUID

data class MentalHealthProfileResponse(
    val id: UUID,
    val userId: UUID,

    val currentFeelingLevel: String?,
    val stressCauses: List<String>,
    val currentConcerns: List<String>,
    val mentalProcessPreferences: List<String>,
    val personalGoals: List<String>,
    val identifiedTriggers: List<String>,

    val personalityAnalysis: String?,
    val emotionalPatterns: String?,
    val riskFactors: String?,
    val identifiedStrengths: String?,
    val recommendedApproach: String?,
    val aiCopingStrategies: List<String>,
    val aiFocusAreas: List<String>,

    val urgencyLevel: UrgencyLevel,
    val supportIntensity: SupportIntensity,
    val communicationStyle: CommunicationStyle?,

    val baselineAnxietyLevel: Int?,
    val baselineDepressionLevel: Int?,
    val baselineStressLevel: Int?,
    val baselineWellbeingLevel: Int?,
    val baselineCopingAbility: Int?,

    val aiAnalysisVersion: String?,
    val aiConfidenceScore: Double?,
    val lastAiAnalysisAt: Long?,

    val createdAt: Long,
    val updatedAt: Long,
)
