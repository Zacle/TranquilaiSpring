package com.tranquilai.user.dto.request

import com.tranquilai.user.entity.CommunicationStyle
import com.tranquilai.user.entity.SupportIntensity
import com.tranquilai.user.entity.UrgencyLevel

data class SaveMentalHealthProfileRequest(
    // Questionnaire answers
    val currentFeelingLevel: String? = null,
    val stressCauses: List<String> = emptyList(),
    val currentConcerns: List<String> = emptyList(),
    val mentalProcessPreferences: List<String> = emptyList(),
    val personalGoals: List<String> = emptyList(),
    val identifiedTriggers: List<String> = emptyList(),

    // AI-generated insights (set by ai-service)
    val personalityAnalysis: String? = null,
    val emotionalPatterns: String? = null,
    val riskFactors: String? = null,
    val identifiedStrengths: String? = null,
    val recommendedApproach: String? = null,
    val aiCopingStrategies: List<String> = emptyList(),
    val aiFocusAreas: List<String> = emptyList(),

    // AI assessment
    val urgencyLevel: UrgencyLevel? = null,
    val supportIntensity: SupportIntensity? = null,
    val communicationStyle: CommunicationStyle? = null,

    // Baseline metrics
    val baselineAnxietyLevel: Int? = null,
    val baselineDepressionLevel: Int? = null,
    val baselineStressLevel: Int? = null,
    val baselineWellbeingLevel: Int? = null,
    val baselineCopingAbility: Int? = null,

    // AI metadata
    val aiAnalysisVersion: String? = null,
    val aiConfidenceScore: Double? = null,
)
