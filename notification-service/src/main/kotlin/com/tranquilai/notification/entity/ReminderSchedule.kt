package com.tranquilai.notification.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(
    name = "reminder_schedules",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "reminder_time"])],
)
class ReminderSchedule(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    /** HH:mm — e.g. "08:00" */
    @Column(name = "reminder_time", nullable = false, length = 5)
    val reminderTime: String,

    /** DAILY | WEEKDAYS | WEEKENDS */
    @Column(nullable = false)
    var frequency: String = "DAILY",

    @Column(nullable = false)
    var enabled: Boolean = true,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = System.currentTimeMillis(),
)
