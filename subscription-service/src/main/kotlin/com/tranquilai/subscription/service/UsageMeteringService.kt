package com.tranquilai.subscription.service

import com.tranquilai.subscription.dto.response.UsageResponse
import com.tranquilai.subscription.repository.UsageRecordRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class UsageMeteringService(
    private val usageRecordRepository: UsageRecordRepository,
    private val subscriptionService: SubscriptionService,
    @param:Value("\${app.free-ai-chat-limit}") private val freeAiChatLimit: Int,
    @param:Value("\${app.free-journal-limit}") private val freeJournalLimit: Int,
) {
    companion object {
        const val FEATURE_AI_CHAT = "AI_CHAT"
        const val FEATURE_JOURNAL_ENTRY = "JOURNAL_ENTRY"
    }

    @Cacheable(value = ["usage"], key = "#userId + ':' + #feature")
    fun checkUsage(userId: UUID, feature: String): UsageResponse {
        val sub = subscriptionService.getSubscriptionForAccessCheck(userId)
        val isPremium = sub.isPremium()
        val plan = sub.planType.name

        // Premium users have unlimited access
        if (isPremium) return UsageResponse(allowed = true, used = 0, limit = null, remaining = null, plan = plan)

        val limit = freeLimitFor(feature) ?: return UsageResponse(allowed = true, used = 0, limit = null, remaining = null, plan = plan)
        val today = LocalDate.now()
        val record = usageRecordRepository.findByUserIdAndFeatureAndUsageDate(userId, feature, today).orElse(null)
        val used = record?.count ?: 0
        val remaining = maxOf(0, limit - used)

        return UsageResponse(
            allowed = used < limit,
            used = used,
            limit = limit,
            remaining = remaining,
            plan = plan,
        )
    }

    @Transactional
    @CacheEvict(value = ["usage"], key = "#userId + ':' + #feature")
    fun incrementUsage(userId: UUID, feature: String) {
        val sub = subscriptionService.getSubscriptionForAccessCheck(userId)
        if (sub.isPremium()) return // no metering for premium

        val limit = freeLimitFor(feature)
        usageRecordRepository.upsertIncrement(userId, feature, LocalDate.now(), limit)
    }

    fun getCurrentUsageSummary(userId: UUID): Map<String, UsageResponse> {
        val features = listOf(FEATURE_AI_CHAT, FEATURE_JOURNAL_ENTRY)
        return features.associateWith { checkUsage(userId, it) }
    }

    private fun freeLimitFor(feature: String): Int? = when (feature) {
        FEATURE_AI_CHAT -> freeAiChatLimit
        FEATURE_JOURNAL_ENTRY -> freeJournalLimit
        else -> null
    }
}
