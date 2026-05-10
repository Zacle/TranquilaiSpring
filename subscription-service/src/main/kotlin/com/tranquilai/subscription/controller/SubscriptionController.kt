package com.tranquilai.subscription.controller

import com.tranquilai.subscription.dto.request.CreateCheckoutRequest
import com.tranquilai.subscription.dto.request.VerifyPlayPurchaseRequest
import com.tranquilai.subscription.dto.response.*
import com.tranquilai.subscription.security.GatewayUser
import com.tranquilai.subscription.service.SubscriptionService
import com.tranquilai.subscription.service.UsageMeteringService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/subscriptions")
class SubscriptionController(
    private val subscriptionService: SubscriptionService,
    private val usageMeteringService: UsageMeteringService,
) {

    /** GET /api/subscriptions/current */
    @GetMapping("/current")
    fun getCurrent(@AuthenticationPrincipal user: GatewayUser): ResponseEntity<SubscriptionResponse> =
        ResponseEntity.ok(subscriptionService.getCurrentSubscription(user.id))

    /** GET /api/subscriptions/plans */
    @GetMapping("/plans")
    fun getPlans(@AuthenticationPrincipal user: GatewayUser): ResponseEntity<List<PlanResponse>> =
        ResponseEntity.ok(subscriptionService.getAvailablePlans())

    /** POST /api/subscriptions/checkout */
    @PostMapping("/checkout")
    fun createCheckout(
        @AuthenticationPrincipal user: GatewayUser,
        @Valid @RequestBody request: CreateCheckoutRequest,
    ): ResponseEntity<CheckoutResponse> =
        ResponseEntity.ok(subscriptionService.createCheckoutSession(user.id, user.email, request))

    /** POST /api/subscriptions/verify-play-purchase */
    @PostMapping("/verify-play-purchase")
    fun verifyPlayPurchase(
        @AuthenticationPrincipal user: GatewayUser,
        @Valid @RequestBody request: VerifyPlayPurchaseRequest,
    ): ResponseEntity<SubscriptionResponse> =
        ResponseEntity.ok(subscriptionService.verifyAndActivatePlayPurchase(user.id, request))

    /** POST /api/subscriptions/cancel */
    @PostMapping("/cancel")
    fun cancel(@AuthenticationPrincipal user: GatewayUser): ResponseEntity<SubscriptionResponse> =
        ResponseEntity.ok(subscriptionService.cancelSubscription(user.id))

    /** POST /api/subscriptions/reactivate */
    @PostMapping("/reactivate")
    fun reactivate(@AuthenticationPrincipal user: GatewayUser): ResponseEntity<SubscriptionResponse> =
        ResponseEntity.ok(subscriptionService.reactivateSubscription(user.id))

    /** GET /api/subscriptions/portal */
    @GetMapping("/portal")
    fun getBillingPortal(@AuthenticationPrincipal user: GatewayUser): ResponseEntity<BillingPortalResponse> =
        ResponseEntity.ok(subscriptionService.getBillingPortalUrl(user.id, user.email))

    /** GET /api/subscriptions/invoices */
    @GetMapping("/invoices")
    fun getInvoices(@AuthenticationPrincipal user: GatewayUser): ResponseEntity<List<InvoiceResponse>> =
        ResponseEntity.ok(subscriptionService.getInvoices(user.id))

    /** GET /api/subscriptions/usage */
    @GetMapping("/usage")
    fun getUsage(@AuthenticationPrincipal user: GatewayUser): ResponseEntity<Map<String, UsageResponse>> =
        ResponseEntity.ok(usageMeteringService.getCurrentUsageSummary(user.id))
}
