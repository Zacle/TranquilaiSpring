package com.tranquilai.subscription.entity

import jakarta.persistence.*
import org.springframework.data.domain.Persistable
import java.time.Instant
import java.util.UUID

enum class PlanType { FREE, PREMIUM_MONTHLY, PREMIUM_ANNUAL }
enum class SubscriptionStatus { ACTIVE, PAST_DUE, CANCELED, EXPIRED, TRIALING }
enum class PaymentProvider { GOOGLE_PLAY }

@Entity
@Table(name = "subscriptions")
class Subscription(
    @Id
    @JvmField
    final val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false, unique = true)
    val userId: UUID,

    @Column(name = "google_play_purchase_token")
    var googlePlayPurchaseToken: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false)
    var planType: PlanType = PlanType.FREE,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: SubscriptionStatus = SubscriptionStatus.ACTIVE,

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_provider")
    var paymentProvider: PaymentProvider? = null,

    @Column(name = "current_period_start")
    var currentPeriodStart: Instant? = null,

    @Column(name = "current_period_end")
    var currentPeriodEnd: Instant? = null,

    @Column(name = "cancel_at_period_end", nullable = false)
    var cancelAtPeriodEnd: Boolean = false,

    @Column(name = "trial_end")
    var trialEnd: Instant? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) : Persistable<UUID> {
    override fun getId(): UUID = id

    @Transient private var newEntity: Boolean = true
    override fun isNew(): Boolean = newEntity
    @PostLoad @PostPersist private fun markNotNew() { newEntity = false }

    fun isPremium(): Boolean {
        val hasPaidPlan = planType != PlanType.FREE
        val hasAccessStatus = status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.TRIALING
        val withinCurrentPeriod = currentPeriodEnd?.isAfter(Instant.now()) ?: true
        return hasPaidPlan && hasAccessStatus && withinCurrentPeriod
    }
}
