package com.tranquilai.subscription.service

import com.tranquilai.subscription.dto.response.EntitlementResponse
import com.tranquilai.subscription.entity.PlanType
import com.tranquilai.subscription.repository.EntitlementGrantRepository
import com.tranquilai.subscription.repository.SubscriptionRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

/**
 * Determines whether a user has access to a given feature based on their plan.
 * Results are cached in Redis for 5 minutes (see AppConfig).
 */
@Service
class EntitlementService(
    private val subscriptionRepository: SubscriptionRepository,
    private val entitlementGrantRepository: EntitlementGrantRepository,
) {

    companion object {
        // Features that are premium-only (unlimited; no metering)
        private val PREMIUM_FEATURES = setOf(
            "ADVANCED_MOOD_INSIGHTS",
            "JOURNAL_SUMMARY",
            "FULL_MEDITATION_LIBRARY",
            "FULL_BREATHING_LIBRARY",
            "CUSTOM_BREATHING_PRESETS",
            "DAILY_WELLNESS_PLAN",
            "DETAILED_PROGRESS_ANALYTICS",
            "PRIORITY_AI",
            "CUSTOM_REMINDERS",
            "EMERGENCY_CONTACTS",
            "EXPORT_DATA",
            "AMBIENT_SOUNDS",
        )
    }

    @Cacheable(value = ["entitlements"], key = "#userId + ':' + #feature")
    fun checkEntitlement(userId: UUID, feature: String): EntitlementResponse {
        val sub = subscriptionRepository.findByUserId(userId).orElse(null)
        val isPremium = sub?.isPremium() ?: false
        val plan = sub?.planType?.name ?: PlanType.FREE.name
        val hasActiveGrant = entitlementGrantRepository.findByUserIdAndFeatureAndGrantedTrue(userId, feature)
            .any {
                val expiresAt = it.expiresAt
                expiresAt == null || expiresAt.isAfter(Instant.now())
            }

        if (hasActiveGrant) {
            return EntitlementResponse(allowed = true, plan = plan)
        }

        // Premium-only features
        if (feature in PREMIUM_FEATURES) {
            return EntitlementResponse(allowed = isPremium, plan = plan)
        }

        // Metered features fall through to UsageMeteringService — always return allowed here
        return EntitlementResponse(allowed = true, plan = plan)
    }

    fun isPremium(userId: UUID): Boolean {
        return subscriptionRepository.findByUserId(userId).orElse(null)?.isPremium() ?: false
    }
}
