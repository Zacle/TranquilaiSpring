package com.tranquilai.user.dto.response

import java.util.UUID

/** Lightweight response used by plan-service to personalise AI-generated daily plans */
data class PlanContextResponse(
    val userId: UUID,
    val firstName: String,
    // Mental health profile (null if questionnaire not yet completed)
    val currentFeelingLevel: String?,
    val stressCauses: List<String>,
    val currentConcerns: List<String>,
    val mentalProcessPreferences: List<String>,
    val personalGoals: List<String>,
    val urgencyLevel: String,
    val supportIntensity: String,
    val communicationStyle: String?,
    val baselineAnxietyLevel: Int?,
    val baselineStressLevel: Int?,
    val baselineWellbeingLevel: Int?,
    val recommendedApproach: String?,
)
