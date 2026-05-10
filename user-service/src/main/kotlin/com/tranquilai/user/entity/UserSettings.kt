package com.tranquilai.user.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "user_settings")
class UserSettings(
    @Id
    val id: UUID = UUID.randomUUID(),

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: User,

    @Column(name = "theme_preference", nullable = false)
    var themePreference: String = "SYSTEM",

    @Column(name = "notifications_enabled", nullable = false)
    var notificationsEnabled: Boolean = true,

    @Column(name = "reminder_enabled", nullable = false)
    var reminderEnabled: Boolean = false,

    /** Comma-separated list of HH:mm reminder times, e.g. "08:00,20:00" */
    @Column(name = "reminder_times", columnDefinition = "TEXT")
    var reminderTimes: String? = null,

    @Column(name = "reminder_frequency", nullable = false)
    var reminderFrequency: String = "DAILY",

    @Column(name = "preferred_content_language", nullable = false)
    var preferredContentLanguage: String = "en",

    @Column(name = "show_explicit_content", nullable = false)
    var showExplicitContent: Boolean = false,

    @Column(name = "created_at", nullable = false)
    val createdAt: Long = System.currentTimeMillis(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = System.currentTimeMillis(),
)
