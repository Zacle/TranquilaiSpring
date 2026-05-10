package com.tranquilai.subscription.service

import com.stripe.model.billingportal.Session as PortalSession
import com.stripe.model.checkout.Session
import com.tranquilai.subscription.config.StripeConfig
import com.tranquilai.subscription.dto.request.CreateCheckoutRequest
import com.tranquilai.subscription.dto.request.VerifyPlayPurchaseRequest
import com.tranquilai.subscription.entity.Invoice
import com.tranquilai.subscription.entity.InvoiceStatus
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
import org.junit.jupiter.api.Assertions.assertTrue
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
    private val stripeService: StripeService = mock(StripeService::class.java)
    private val playBillingService: PlayBillingService = mock(PlayBillingService::class.java)
    private val cacheService: SubscriptionCacheService = mock(SubscriptionCacheService::class.java)
    private val stripeConfig = StripeConfig("sk_test", "whsec", "price_monthly", "price_annual", "https://return")
    private val service = SubscriptionService(subscriptionRepository, invoiceRepository, stripeService, playBillingService, cacheService, stripeConfig, 7)

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
    fun `available plans use configured Stripe price ids and trial days`() {
        val plans = service.getAvailablePlans()

        assertEquals(listOf("price_monthly", "price_annual"), plans.map { it.priceId })
        assertEquals(listOf(7, 7), plans.map { it.trialDays })
    }

    @Test
    fun `createCheckoutSession rejects unknown prices and active subscriptions`() {
        val userId = UUID.randomUUID()
        assertThrows(SubscriptionException::class.java) {
            service.createCheckoutSession(userId, "user@example.com", CreateCheckoutRequest("unknown"))
        }

        val active = Subscription(userId = userId, planType = PlanType.PREMIUM_MONTHLY, paymentProvider = PaymentProvider.STRIPE)
        `when`(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(active))
        assertThrows(SubscriptionException::class.java) {
            service.createCheckoutSession(userId, "user@example.com", CreateCheckoutRequest("price_monthly"))
        }
    }

    @Test
    fun `createCheckoutSession creates customer when needed and returns session details`() {
        val userId = UUID.randomUUID()
        val sub = Subscription(userId = userId)
        val session: Session = mock(Session::class.java)
        `when`(session.id).thenReturn("cs_123")
        `when`(session.url).thenReturn("https://checkout")
        `when`(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(sub))
        `when`(subscriptionRepository.save(sub)).thenReturn(sub)
        `when`(stripeService.createCustomer("user@example.com", userId.toString())).thenReturn("cus_123")
        `when`(stripeService.createCheckoutSession("cus_123", "price_monthly", "success", "cancel", 7)).thenReturn(session)

        val response = service.createCheckoutSession(userId, "user@example.com", CreateCheckoutRequest("price_monthly", "success", "cancel"))

        assertEquals("cs_123", response.sessionId)
        assertEquals("https://checkout", response.checkoutUrl)
        assertEquals("cus_123", sub.stripeCustomerId)
    }

    @Test
    fun `billing portal routes by payment provider`() {
        val userId = UUID.randomUUID()
        val play = Subscription(userId = userId, planType = PlanType.PREMIUM_MONTHLY, paymentProvider = PaymentProvider.GOOGLE_PLAY)
        `when`(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(play))
        assertEquals(PaymentProvider.GOOGLE_PLAY.name, service.getBillingPortalUrl(userId, "user@example.com").paymentProvider)

        val stripe = Subscription(userId = userId, stripeCustomerId = "cus_123", paymentProvider = PaymentProvider.STRIPE)
        val portal: PortalSession = mock(PortalSession::class.java)
        `when`(portal.url).thenReturn("https://portal")
        `when`(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(stripe))
        `when`(stripeService.createBillingPortalSession("cus_123", "https://return")).thenReturn(portal)
        assertEquals("https://portal", service.getBillingPortalUrl(userId, "user@example.com").portalUrl)
    }

    @Test
    fun `cancel and reactivate Stripe subscription update flags and evict cache`() {
        val userId = UUID.randomUUID()
        val sub = Subscription(
            userId = userId,
            stripeSubscriptionId = "sub_123",
            planType = PlanType.PREMIUM_MONTHLY,
            paymentProvider = PaymentProvider.STRIPE,
        )
        `when`(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(sub))
        `when`(subscriptionRepository.save(sub)).thenReturn(sub)

        val canceled = service.cancelSubscription(userId)
        assertTrue(canceled.cancelAtPeriodEnd)
        verify(stripeService).cancelAtPeriodEnd("sub_123")

        val reactivated = service.reactivateSubscription(userId)
        assertEquals(false, reactivated.cancelAtPeriodEnd)
        verify(stripeService).reactivate("sub_123")
        verify(cacheService, org.mockito.Mockito.times(2)).evictUser(userId)
    }

    @Test
    fun `cancel rejects missing free and Google Play subscriptions`() {
        val userId = UUID.randomUUID()
        `when`(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.empty())
        assertThrows(ResourceNotFoundException::class.java) { service.cancelSubscription(userId) }

        `when`(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(Subscription(userId = userId)))
        assertThrows(SubscriptionException::class.java) { service.cancelSubscription(userId) }

        val play = Subscription(userId = userId, planType = PlanType.PREMIUM_MONTHLY, paymentProvider = PaymentProvider.GOOGLE_PLAY)
        `when`(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(play))
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
    fun `webhook handlers update subscription and invoices idempotently`() {
        val userId = UUID.randomUUID()
        val sub = Subscription(userId = userId)
        `when`(subscriptionRepository.findByUserId(userId)).thenReturn(Optional.of(sub))
        `when`(stripeService.priceIdToPlanType("price_annual")).thenReturn(PlanType.PREMIUM_ANNUAL)

        service.handleSubscriptionActivated(userId, "sub_123", "cus_123", "price_annual", Instant.EPOCH, Instant.now().plusSeconds(100), null, false, false)

        assertEquals(PlanType.PREMIUM_ANNUAL, sub.planType)
        assertEquals(PaymentProvider.STRIPE, sub.paymentProvider)
        verify(cacheService).evictUser(userId)

        `when`(invoiceRepository.findByStripeInvoiceId("in_1")).thenReturn(Optional.empty())
        service.recordInvoicePaid(userId, sub.id, "in_1", 999, "USD")
        verify(invoiceRepository).save(anyInvoice())

        `when`(invoiceRepository.findByStripeInvoiceId("in_1")).thenReturn(Optional.of(Invoice(userId = userId, stripeInvoiceId = "in_1", amountCents = 999, status = InvoiceStatus.PAID)))
        service.recordInvoicePaid(userId, sub.id, "in_1", 999, "USD")
        verify(invoiceRepository, org.mockito.Mockito.times(1)).save(anyInvoice())
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

    private fun anyInvoice(): Invoice {
        any(Invoice::class.java)
        return uninitialized()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> uninitialized(): T = null as T
}
