package com.tranquilai.subscription.service

import com.tranquilai.subscription.dto.request.VerifyPlayPurchaseRequest
import com.tranquilai.subscription.entity.PaymentProvider
import com.tranquilai.subscription.entity.PlanType
import com.tranquilai.subscription.entity.Subscription
import com.tranquilai.subscription.entity.SubscriptionStatus
import com.tranquilai.subscription.exception.ResourceNotFoundException
import com.tranquilai.subscription.exception.SubscriptionException
import com.tranquilai.subscription.repository.InvoiceRepository
import com.tranquilai.subscription.repository.SubscriptionRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Instant
import java.util.Optional
import java.util.UUID

class SubscriptionServiceTest {

    private val subscriptionRepository: SubscriptionRepository = mock(SubscriptionRepository::class.java)
    private val invoiceRepository: InvoiceRepository = mock(InvoiceRepository::class.java)
    private val playBillingService: PlayBillingService = mock(PlayBillingService::class.java)
    private val cacheService: SubscriptionCacheService = mock(SubscriptionCacheService::class.java)
    private val service = SubscriptionService(subscriptionRepository, invoiceRepository, playBillingService, cacheService, 7)

    @Test
    fun `getCurrentSubscription creates free subscription when missing`() {
        val userId = UUID.randomUUID()
        `when`(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.empty())
        `when`(subscriptionRepository.save(anySubscription())).thenAnswer { it.getArgument<Subscription>(0) }

        val response = service.getCurrentSubscription(userId)

        assertEquals(PlanType.FREE, response.planType)
        assertEquals(false, response.isPremium)
    }

    @Test
    fun `available plans expose Google Play product ids and trial days`() {
        val plans = service.getAvailablePlans()

        assertEquals(listOf("tranquilai_premium_monthly", "tranquilai_premium_annual"), plans.map { it.priceId })
        assertEquals(listOf(7, 7), plans.map { it.trialDays })
        assertEquals(listOf(299, 2399), plans.map { it.amountCents })
    }

    @Test
    fun `billing portal returns Google Play or free-plan management message`() {
        val userId = UUID.randomUUID()
        val play = Subscription(
            userId = userId,
            planType = PlanType.PREMIUM_MONTHLY,
            paymentProvider = PaymentProvider.GOOGLE_PLAY,
        )
        `when`(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(play))
        assertEquals(PaymentProvider.GOOGLE_PLAY.name, service.getBillingPortalUrl(userId, "user@example.com").paymentProvider)

        val freeUserId = UUID.randomUUID()
        `when`(subscriptionRepository.findByUserId(freeUserId)).thenReturn(Optional.empty())
        `when`(subscriptionRepository.save(anySubscription())).thenAnswer { it.getArgument<Subscription>(0) }
        assertEquals("NONE", service.getBillingPortalUrl(freeUserId, "user@example.com").paymentProvider)
    }

    @Test
    fun `cancel and reactivate direct users to Google Play`() {
        val userId = UUID.randomUUID()
        val active = Subscription(
            userId = userId,
            planType = PlanType.PREMIUM_MONTHLY,
            paymentProvider = PaymentProvider.GOOGLE_PLAY,
        )
        `when`(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(active))

        assertThrows(SubscriptionException::class.java) { service.cancelSubscription(userId) }

        active.cancelAtPeriodEnd = true
        assertThrows(SubscriptionException::class.java) { service.reactivateSubscription(userId) }
    }

    @Test
    fun `cancel rejects missing and free subscriptions`() {
        val userId = UUID.randomUUID()
        `when`(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.empty())
        assertThrows(ResourceNotFoundException::class.java) { service.cancelSubscription(userId) }

        `when`(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(Subscription(userId = userId)))
        assertThrows(SubscriptionException::class.java) { service.cancelSubscription(userId) }
    }

    @Test
    fun `verifyAndActivatePlayPurchase activates Google Play subscription`() {
        val userId = UUID.randomUUID()
        val request = VerifyPlayPurchaseRequest("token", "tranquilai_premium_annual")
        val sub = Subscription(userId = userId)
        val expires = Instant.now().plusSeconds(3600)
        `when`(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(sub))
        `when`(playBillingService.verifySubscription(request)).thenReturn(
            VerifiedPlayPurchase(PlanType.PREMIUM_ANNUAL, Instant.now(), expires, autoRenewing = false),
        )
        `when`(subscriptionRepository.save(sub)).thenReturn(sub)

        val response = service.verifyAndActivatePlayPurchase(userId, request)

        assertEquals(PlanType.PREMIUM_ANNUAL, response.planType)
        assertEquals(PaymentProvider.GOOGLE_PLAY.name, response.paymentProvider)
        assertEquals(true, response.cancelAtPeriodEnd)
        verify(cacheService).evictUser(userId)
    }

    @Test
    fun `verifyAndActivatePlayPurchase rejects already premium users`() {
        val userId = UUID.randomUUID()
        val request = VerifyPlayPurchaseRequest("token", "tranquilai_premium_monthly")
        val premium = Subscription(
            userId = userId,
            planType = PlanType.PREMIUM_MONTHLY,
            paymentProvider = PaymentProvider.GOOGLE_PLAY,
            currentPeriodEnd = Instant.now().plusSeconds(3600),
        )
        `when`(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(premium))
        `when`(playBillingService.verifySubscription(request)).thenReturn(
            VerifiedPlayPurchase(PlanType.PREMIUM_MONTHLY, Instant.now(), Instant.now().plusSeconds(3600), true),
        )

        assertThrows(SubscriptionException::class.java) {
            service.verifyAndActivatePlayPurchase(userId, request)
        }
    }

    @Test
    fun `Google Play notifications update matching subscription and ignore unknown token`() {
        val userId = UUID.randomUUID()
        val sub = Subscription(userId = userId, googlePlayPurchaseToken = "token", planType = PlanType.PREMIUM_MONTHLY)
        `when`(subscriptionRepository.findByGooglePlayPurchaseToken("missing")).thenReturn(Optional.empty())
        service.handleGooglePlayNotification("monthly", "missing", 4)
        verify(subscriptionRepository, never()).save(sub)

        `when`(subscriptionRepository.findByGooglePlayPurchaseToken("token")).thenReturn(Optional.of(sub))
        service.handleGooglePlayNotification("monthly", "token", 4)
        assertEquals(SubscriptionStatus.EXPIRED, sub.status)
        verify(cacheService).evictUser(userId)
    }

    private fun anySubscription(): Subscription {
        any(Subscription::class.java)
        return uninitialized()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> uninitialized(): T = null as T
}
