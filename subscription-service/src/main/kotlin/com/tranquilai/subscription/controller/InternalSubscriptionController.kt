package com.tranquilai.subscription.controller

import com.tranquilai.subscription.dto.request.IncrementUsageRequest
import com.tranquilai.subscription.dto.response.EntitlementResponse
import com.tranquilai.subscription.dto.response.SubscriptionResponse
import com.tranquilai.subscription.dto.response.UsageResponse
import com.tranquilai.subscription.service.EntitlementService
import com.tranquilai.subscription.service.SubscriptionService
import com.tranquilai.subscription.service.UsageMeteringService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * Internal API — only accessible from other microservices via X-Internal-Key.
 * Blocked at the gateway for external clients.
 */
@RestController
@RequestMapping("/internal/subscriptions")
@PreAuthorize("hasRole('INTERNAL')")
class InternalSubscriptionController(
    private val subscriptionService: SubscriptionService,
    private val entitlementService: EntitlementService,
    private val usageMeteringService: UsageMeteringService,
) {

    /** GET /internal/subscriptions/entitlement?userId=&feature= */
    @GetMapping("/entitlement")
    fun checkEntitlement(
        @RequestParam userId: UUID,
        @RequestParam feature: String,
    ): ResponseEntity<EntitlementResponse> =
        ResponseEntity.ok(entitlementService.checkEntitlement(userId, feature))

    /** GET /internal/subscriptions/usage?userId=&feature= */
    @GetMapping("/usage")
    fun checkUsage(
        @RequestParam userId: UUID,
        @RequestParam feature: String,
    ): ResponseEntity<UsageResponse> =
        ResponseEntity.ok(usageMeteringService.checkUsage(userId, feature))

    /** POST /internal/subscriptions/usage/increment */
    @PostMapping("/usage/increment")
    fun incrementUsage(
        @RequestParam userId: UUID,
        @Valid @RequestBody request: IncrementUsageRequest,
    ): ResponseEntity<Void> {
        usageMeteringService.incrementUsage(userId, request.feature)
        return ResponseEntity.noContent().build()
    }

    /** GET /internal/subscriptions/user/{userId} */
    @GetMapping("/user/{userId}")
    fun getSubscriptionByUserId(@PathVariable userId: UUID): ResponseEntity<SubscriptionResponse> {
        val sub = subscriptionService.getOrCreateFreeSubscription(userId)
        return ResponseEntity.ok(
            SubscriptionResponse(
                id = sub.id,
                userId = sub.userId,
                planType = sub.planType,
                status = sub.status,
                isPremium = sub.isPremium(),
                currentPeriodStart = sub.currentPeriodStart,
                currentPeriodEnd = sub.currentPeriodEnd,
                cancelAtPeriodEnd = sub.cancelAtPeriodEnd,
                trialEnd = sub.trialEnd,
                paymentProvider = sub.paymentProvider?.name,
                createdAt = sub.createdAt,
            ),
        )
    }

    /** DELETE /internal/subscriptions/user/{userId} */
    @DeleteMapping("/user/{userId}")
    fun deleteSubscriptionData(@PathVariable userId: UUID): ResponseEntity<Void> {
        subscriptionService.deleteAccountSubscriptionData(userId)
        return ResponseEntity.noContent().build()
    }
}
