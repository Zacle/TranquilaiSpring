package com.tranquilai.subscription.controller

import com.tranquilai.subscription.dto.request.IncrementUsageRequest
import com.tranquilai.subscription.dto.request.VerifyPlayPurchaseRequest
import com.tranquilai.subscription.dto.response.BillingPortalResponse
import com.tranquilai.subscription.dto.response.EntitlementResponse
import com.tranquilai.subscription.dto.response.InvoiceResponse
import com.tranquilai.subscription.dto.response.PlanResponse
import com.tranquilai.subscription.dto.response.SubscriptionResponse
import com.tranquilai.subscription.dto.response.UsageResponse
import com.tranquilai.subscription.entity.PlanType
import com.tranquilai.subscription.entity.Subscription
import com.tranquilai.subscription.entity.SubscriptionStatus
import com.tranquilai.subscription.security.GatewayUser
import com.tranquilai.subscription.service.EntitlementService
import com.tranquilai.subscription.service.SubscriptionService
import com.tranquilai.subscription.service.UsageMeteringService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus
import java.time.Instant
import java.util.UUID

class SubscriptionControllersTest {

    private val user = GatewayUser(UUID.randomUUID(), "user@example.com", "USER")

    @Test
    fun `subscription controller delegates public endpoints`() {
        val subscriptionService: SubscriptionService = mock(SubscriptionService::class.java)
        val usageService: UsageMeteringService = mock(UsageMeteringService::class.java)
        val controller = SubscriptionController(subscriptionService, usageService)
        val current = subscriptionResponse(user.id)
        val playRequest = VerifyPlayPurchaseRequest("token", "product")
        `when`(subscriptionService.getCurrentSubscription(user.id)).thenReturn(current)
        `when`(subscriptionService.getAvailablePlans()).thenReturn(listOf(PlanResponse("p", "Plan", "price", 999, "USD", "month", 7)))
        `when`(subscriptionService.verifyAndActivatePlayPurchase(user.id, playRequest)).thenReturn(current)
        `when`(subscriptionService.cancelSubscription(user.id)).thenReturn(current)
        `when`(subscriptionService.reactivateSubscription(user.id)).thenReturn(current)
        `when`(subscriptionService.getBillingPortalUrl(user.id, user.email)).thenReturn(BillingPortalResponse("NONE"))
        `when`(subscriptionService.getInvoices(user.id)).thenReturn(listOf(InvoiceResponse(UUID.randomUUID(), 999, "USD", "PAID", "GOOGLE_PLAY", Instant.EPOCH, Instant.EPOCH)))
        `when`(usageService.getCurrentUsageSummary(user.id)).thenReturn(mapOf("AI_CHAT" to UsageResponse(true, 0, 3, 3, "FREE")))

        assertEquals(current, controller.getCurrent(user).body)
        assertEquals(1, controller.getPlans(user).body?.size)
        assertEquals(current, controller.verifyPlayPurchase(user, playRequest).body)
        assertEquals(current, controller.cancel(user).body)
        assertEquals(current, controller.reactivate(user).body)
        assertEquals("NONE", controller.getBillingPortal(user).body?.paymentProvider)
        assertEquals(1, controller.getInvoices(user).body?.size)
        assertEquals(true, controller.getUsage(user).body?.get("AI_CHAT")?.allowed)
    }

    @Test
    fun `internal subscription controller delegates entitlement usage and user lookup`() {
        val subscriptionService: SubscriptionService = mock(SubscriptionService::class.java)
        val entitlementService: EntitlementService = mock(EntitlementService::class.java)
        val usageService: UsageMeteringService = mock(UsageMeteringService::class.java)
        val controller = InternalSubscriptionController(subscriptionService, entitlementService, usageService)
        val sub = Subscription(userId = user.id)
        `when`(entitlementService.checkEntitlement(user.id, "EXPORT_DATA")).thenReturn(EntitlementResponse(true, plan = "FREE"))
        `when`(usageService.checkUsage(user.id, "AI_CHAT")).thenReturn(UsageResponse(true, 0, 3, 3, "FREE"))
        `when`(subscriptionService.getSubscriptionForAccessCheck(user.id)).thenReturn(sub)

        assertEquals(true, controller.checkEntitlement(user.id, "EXPORT_DATA").body?.allowed)
        assertEquals(3, controller.checkUsage(user.id, "AI_CHAT").body?.remaining)
        assertEquals(HttpStatus.NO_CONTENT, controller.incrementUsage(user.id, IncrementUsageRequest("AI_CHAT")).statusCode)
        assertEquals(sub.id, controller.getSubscriptionByUserId(user.id).body?.id)
        verify(usageService).incrementUsage(user.id, "AI_CHAT")
    }

    private fun subscriptionResponse(userId: UUID) = SubscriptionResponse(
        id = UUID.randomUUID(),
        userId = userId,
        planType = PlanType.FREE,
        status = SubscriptionStatus.ACTIVE,
        isPremium = false,
        currentPeriodStart = null,
        currentPeriodEnd = null,
        cancelAtPeriodEnd = false,
        trialEnd = null,
        paymentProvider = null,
        createdAt = Instant.EPOCH,
    )
}
