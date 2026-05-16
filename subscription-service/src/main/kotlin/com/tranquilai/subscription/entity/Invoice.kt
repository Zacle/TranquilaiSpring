package com.tranquilai.subscription.entity

import jakarta.persistence.*
import org.springframework.data.domain.Persistable
import java.time.Instant
import java.util.UUID

enum class InvoiceStatus { PAID, PENDING, FAILED, REFUNDED }

@Entity
@Table(name = "invoices")
class Invoice(
    @Id
    @JvmField
    final val id: UUID = UUID.randomUUID(),

    @Column(name = "subscription_id")
    val subscriptionId: UUID? = null,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "amount_cents", nullable = false)
    val amountCents: Int,

    @Column(name = "currency", nullable = false)
    val currency: String = "USD",

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: InvoiceStatus,

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_provider")
    val paymentProvider: PaymentProvider? = null,

    @Column(name = "paid_at")
    var paidAt: Instant? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
) : Persistable<UUID> {
    override fun getId(): UUID = id

    @Transient private var newEntity: Boolean = true
    override fun isNew(): Boolean = newEntity
    @PostLoad @PostPersist private fun markNotNew() { newEntity = false }
}
