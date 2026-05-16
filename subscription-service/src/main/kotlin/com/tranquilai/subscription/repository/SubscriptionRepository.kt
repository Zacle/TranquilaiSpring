package com.tranquilai.subscription.repository

import com.tranquilai.subscription.entity.Subscription
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface SubscriptionRepository : JpaRepository<Subscription, UUID> {
    fun findByUserId(userId: UUID): Optional<Subscription>
    fun findByGooglePlayPurchaseToken(googlePlayPurchaseToken: String): Optional<Subscription>
}
