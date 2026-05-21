package com.tranquilai.auth.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "auth_outbox_events")
class OutboxEvent(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "aggregate_type", nullable = false)
    val aggregateType: String,

    @Column(name = "aggregate_id", nullable = false)
    val aggregateId: String,

    @Column(name = "event_type", nullable = false)
    val eventType: String,

    @Column(name = "routing_key", nullable = false)
    val routingKey: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val payload: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OutboxEventStatus = OutboxEventStatus.PENDING,

    @Column(nullable = false)
    var attempts: Int = 0,

    @Column(name = "last_error", columnDefinition = "TEXT")
    var lastError: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Long = System.currentTimeMillis(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = System.currentTimeMillis(),

    @Column(name = "next_attempt_at", nullable = false)
    var nextAttemptAt: Long = System.currentTimeMillis(),

    @Column(name = "published_at")
    var publishedAt: Long? = null,
) {
    fun markPublished(now: Long = System.currentTimeMillis()) {
        status = OutboxEventStatus.PUBLISHED
        publishedAt = now
        updatedAt = now
        lastError = null
    }

    fun markRetry(error: String, nextAttemptAt: Long, now: Long = System.currentTimeMillis()) {
        attempts += 1
        lastError = error.take(2_000)
        this.nextAttemptAt = nextAttemptAt
        updatedAt = now
    }

    fun markFailed(error: String, now: Long = System.currentTimeMillis()) {
        attempts += 1
        status = OutboxEventStatus.FAILED
        lastError = error.take(2_000)
        updatedAt = now
    }
}

enum class OutboxEventStatus {
    PENDING,
    PUBLISHED,
    FAILED,
}
