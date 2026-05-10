package com.tranquilai.subscription.service

import com.tranquilai.subscription.entity.EntitlementGrant
import com.tranquilai.subscription.entity.PlanType
import com.tranquilai.subscription.entity.Subscription
import com.tranquilai.subscription.entity.UsageRecord
import com.tranquilai.subscription.repository.EntitlementGrantRepository
import com.tranquilai.subscription.repository.SubscriptionRepository
import com.tranquilai.subscription.repository.UsageRecordRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Instant
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class UsageAndEntitlementServiceTest {

    @Test
    fun `usage check enforces free limits and summarizes known features`() {
        val userId = UUID.randomUUID()
        val usageRepo: UsageRecordRepository = mock(UsageRecordRepository::class.java)
        val subscriptionRepo: SubscriptionRepository = mock(SubscriptionRepository::class.java)
        val service = UsageMeteringService(usageRepo, subscriptionRepo, freeAiChatLimit = 3, freeJournalLimit = 2)
        `when`(subscriptionRepo.findByUserId(userId)).thenReturn(Optional.empty())
        `when`(usageRepo.findByUserIdAndFeatureAndUsageDate(userId, "AI_CHAT", LocalDate.now()))
            .thenReturn(Optional.of(UsageRecord(userId = userId, feature = "AI_CHAT", count = 3)))
        `when`(usageRepo.findByUserIdAndFeatureAndUsageDate(userId, "JOURNAL_ENTRY", LocalDate.now()))
            .thenReturn(Optional.of(UsageRecord(userId = userId, feature = "JOURNAL_ENTRY", count = 1)))

        val ai = service.checkUsage(userId, "AI_CHAT")
        val summary = service.getCurrentUsageSummary(userId)

        assertFalse(ai.allowed)
        assertEquals(0, ai.remaining)
        assertTrue(summary["JOURNAL_ENTRY"]!!.allowed)
        assertEquals(1, summary["JOURNAL_ENTRY"]!!.remaining)
    }

    @Test
    fun `usage check and increment bypass metering for premium users`() {
        val userId = UUID.randomUUID()
        val usageRepo: UsageRecordRepository = mock(UsageRecordRepository::class.java)
        val subscriptionRepo: SubscriptionRepository = mock(SubscriptionRepository::class.java)
        val premium = Subscription(userId = userId, planType = PlanType.PREMIUM_MONTHLY)
        val service = UsageMeteringService(usageRepo, subscriptionRepo, 3, 2)
        `when`(subscriptionRepo.findByUserId(userId)).thenReturn(Optional.of(premium))

        val response = service.checkUsage(userId, "AI_CHAT")
        service.incrementUsage(userId, "AI_CHAT")

        assertTrue(response.allowed)
        assertEquals(null, response.limit)
        verify(usageRepo, never()).upsertIncrement(userId, "AI_CHAT", LocalDate.now(), 3)
    }

    @Test
    fun `incrementUsage delegates capped upsert for free users`() {
        val userId = UUID.randomUUID()
        val usageRepo: UsageRecordRepository = mock(UsageRecordRepository::class.java)
        val subscriptionRepo: SubscriptionRepository = mock(SubscriptionRepository::class.java)
        `when`(subscriptionRepo.findByUserId(userId)).thenReturn(Optional.empty())

        UsageMeteringService(usageRepo, subscriptionRepo, 3, 2).incrementUsage(userId, "JOURNAL_ENTRY")

        verify(usageRepo).upsertIncrement(userId, "JOURNAL_ENTRY", LocalDate.now(), 2)
    }

    @Test
    fun `entitlement allows active grants and premium-only features for premium users`() {
        val userId = UUID.randomUUID()
        val subscriptionRepo: SubscriptionRepository = mock(SubscriptionRepository::class.java)
        val grantRepo: EntitlementGrantRepository = mock(EntitlementGrantRepository::class.java)
        val service = EntitlementService(subscriptionRepo, grantRepo)
        `when`(subscriptionRepo.findByUserId(userId)).thenReturn(Optional.empty())
        `when`(grantRepo.findByUserIdAndFeatureAndGrantedTrue(userId, "EXPORT_DATA"))
            .thenReturn(listOf(EntitlementGrant(userId = userId, feature = "EXPORT_DATA", expiresAt = Instant.now().plusSeconds(60))))

        assertTrue(service.checkEntitlement(userId, "EXPORT_DATA").allowed)

        `when`(grantRepo.findByUserIdAndFeatureAndGrantedTrue(userId, "ADVANCED_MOOD_INSIGHTS")).thenReturn(emptyList())
        assertFalse(service.checkEntitlement(userId, "ADVANCED_MOOD_INSIGHTS").allowed)

        val premium = Subscription(userId = userId, planType = PlanType.PREMIUM_ANNUAL)
        `when`(subscriptionRepo.findByUserId(userId)).thenReturn(Optional.of(premium))
        assertTrue(service.checkEntitlement(userId, "ADVANCED_MOOD_INSIGHTS").allowed)
        assertTrue(service.isPremium(userId))
    }
}
