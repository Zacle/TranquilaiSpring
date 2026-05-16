package com.tranquilai.subscription.entity

import jakarta.persistence.*
import org.springframework.data.domain.Persistable
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(
    name = "usage_records",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "feature", "usage_date"])],
)
class UsageRecord(
    @Id
    @JvmField
    final val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "feature", nullable = false)
    val feature: String,

    @Column(name = "usage_date", nullable = false)
    val usageDate: LocalDate = LocalDate.now(),

    @Column(name = "count", nullable = false)
    var count: Int = 0,

    @Column(name = "daily_limit")
    val dailyLimit: Int? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
) : Persistable<UUID> {
    override fun getId(): UUID = id

    @Transient private var newEntity: Boolean = true
    override fun isNew(): Boolean = newEntity
    @PostLoad @PostPersist private fun markNotNew() { newEntity = false }
}
