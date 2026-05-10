package com.tranquilai.subscription.repository

import com.tranquilai.subscription.entity.Invoice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface InvoiceRepository : JpaRepository<Invoice, UUID> {
    fun findByUserIdOrderByCreatedAtDesc(userId: UUID): List<Invoice>
    fun findByStripeInvoiceId(stripeInvoiceId: String): Optional<Invoice>
}
