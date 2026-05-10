package com.tranquilai.subscription.repository

import com.tranquilai.subscription.entity.EntitlementGrant
import com.tranquilai.subscription.entity.Invoice
import com.tranquilai.subscription.entity.InvoiceStatus
import com.tranquilai.subscription.entity.PlanType
import com.tranquilai.subscription.entity.Subscription
import com.tranquilai.subscription.entity.UsageRecord
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate
import java.util.UUID

@DataJpaTest
@TestPropertySource(
    properties = [
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false",
    ],
)
class SubscriptionRepositoryIntegrationTest @Autowired constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val invoiceRepository: InvoiceRepository,
    private val usageRecordRepository: UsageRecordRepository,
    private val entitlementGrantRepository: EntitlementGrantRepository,
) {

    @Test
    fun `subscription repository finds by user customer subscription and play token`() {
        val userId = UUID.randomUUID()
        val sub = subscriptionRepository.save(
            Subscription(
                userId = userId,
                stripeCustomerId = "cus_123",
                stripeSubscriptionId = "sub_123",
                googlePlayPurchaseToken = "play_token",
                planType = PlanType.PREMIUM_MONTHLY,
            ),
        )

        assertEquals(sub.id, subscriptionRepository.findByUserId(userId).get().id)
        assertEquals(sub.id, subscriptionRepository.findByStripeCustomerId("cus_123").get().id)
        assertEquals(sub.id, subscriptionRepository.findByStripeSubscriptionId("sub_123").get().id)
        assertEquals(sub.id, subscriptionRepository.findByGooglePlayPurchaseToken("play_token").get().id)
    }

    @Test
    fun `invoice repository orders invoices and finds stripe invoice id`() {
        val userId = UUID.randomUUID()
        val old = invoiceRepository.save(Invoice(userId = userId, stripeInvoiceId = "old", amountCents = 100, status = InvoiceStatus.PAID))
        Thread.sleep(2)
        val newest = invoiceRepository.save(Invoice(userId = userId, stripeInvoiceId = "new", amountCents = 200, status = InvoiceStatus.FAILED))

        assertEquals(listOf(newest.id, old.id), invoiceRepository.findByUserIdOrderByCreatedAtDesc(userId).map { it.id })
        assertEquals(newest.id, invoiceRepository.findByStripeInvoiceId("new").get().id)
    }

    @Test
    fun `usage and entitlement repositories find current records`() {
        val userId = UUID.randomUUID()
        val today = LocalDate.now()
        val usage = usageRecordRepository.save(UsageRecord(userId = userId, feature = "AI_CHAT", usageDate = today, count = 2))
        entitlementGrantRepository.save(EntitlementGrant(userId = userId, feature = "EXPORT_DATA", granted = true))
        entitlementGrantRepository.save(EntitlementGrant(userId = userId, feature = "EXPORT_DATA", granted = false))

        assertEquals(usage.id, usageRecordRepository.findByUserIdAndFeatureAndUsageDate(userId, "AI_CHAT", today).get().id)
        assertEquals(1, entitlementGrantRepository.findByUserIdAndFeatureAndGrantedTrue(userId, "EXPORT_DATA").size)
        assertTrue(usageRecordRepository.findByUserIdAndFeatureAndUsageDate(userId, "AI_CHAT", today.minusDays(1)).isEmpty)
    }
}
