package com.tranquilai.user.dto.request

import com.tranquilai.user.entity.OnboardingStatus
import jakarta.validation.constraints.NotNull

data class UpdateOnboardingStatusRequest(
    @field:NotNull(message = "Onboarding status is required")
    val status: OnboardingStatus,
)
