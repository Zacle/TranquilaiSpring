package com.tranquilai.notification.entity

import jakarta.persistence.*
import org.springframework.data.domain.Persistable
import java.util.UUID

@Entity
@Table(name = "notification_logs")
class NotificationLog(
    @Id
    @JvmField
    final val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val body: String,

    @Column(name = "notification_type", nullable = false)
    val notificationType: String,

    @Column(nullable = false)
    var status: String,           // SENT | FAILED | SKIPPED

    @Column(name = "error_message", columnDefinition = "TEXT")
    var errorMessage: String? = null,

    @Column(name = "device_token", columnDefinition = "TEXT")
    val deviceToken: String? = null,

    @Column(name = "sent_at", nullable = false)
    val sentAt: Long = System.currentTimeMillis(),
) : Persistable<UUID> {
    override fun getId(): UUID = id

    @Transient private var newEntity: Boolean = true
    override fun isNew(): Boolean = newEntity
    @PostLoad @PostPersist private fun markNotNew() { newEntity = false }
}
