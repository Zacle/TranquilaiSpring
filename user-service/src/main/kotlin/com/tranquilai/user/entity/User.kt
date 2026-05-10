package com.tranquilai.user.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "users")
class User(
    @Id
    val id: UUID,

    @Column(nullable = false, unique = true)
    var email: String,

    @Column(nullable = false, unique = true)
    var username: String,

    @Column(name = "first_name", nullable = false)
    var firstName: String,

    @Column(name = "last_name", nullable = false)
    var lastName: String,

    @Column(name = "date_of_birth")
    var dateOfBirth: Long? = null,

    @Column(name = "phone_number")
    var phoneNumber: String? = null,

    var timezone: String? = null,

    @Column(name = "language_preference", nullable = false)
    var languagePreference: String = "en",

    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_status", nullable = false)
    var onboardingStatus: OnboardingStatus = OnboardingStatus.NOT_STARTED,

    @Column(name = "profile_picture_url")
    var profilePictureUrl: String? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: Long = System.currentTimeMillis(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = System.currentTimeMillis(),

    @OneToOne(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var mentalHealthProfile: MentalHealthProfile? = null,
) {
    val fullName: String get() = "$firstName $lastName".trim()
}

enum class OnboardingStatus { NOT_STARTED, IN_PROGRESS, COMPLETED }
