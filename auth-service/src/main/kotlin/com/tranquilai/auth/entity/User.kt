package com.tranquilai.auth.entity

import jakarta.persistence.*
import org.springframework.data.domain.Persistable
import java.util.UUID

@Entity
@Table(name = "users")
class User(
    @Id
    @JvmField
    final val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true)
    var email: String,

    @Column(nullable = false, unique = true)
    var username: String,

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,

    @Column(name = "first_name", nullable = false)
    var firstName: String,

    @Column(name = "last_name", nullable = false)
    var lastName: String,

    @Column(name = "date_of_birth")
    var dateOfBirth: Long? = null,

    @Column(name = "phone_number")
    var phoneNumber: String? = null,

    var timezone: String? = null,

    @Column(name = "language_preference")
    var languagePreference: String = "en",

    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_status", nullable = false)
    var onboardingStatus: OnboardingStatus = OnboardingStatus.NOT_STARTED,

    @Column(name = "profile_picture_url")
    var profilePictureUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false)
    var authProvider: AuthProvider = AuthProvider.PASSWORD,

    @Column(name = "google_subject", unique = true)
    var googleSubject: String? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "is_email_verified", nullable = false)
    var isEmailVerified: Boolean = false,

    @Column(nullable = false)
    var roles: String = UserRole.USER.name,

    @Column(name = "created_at", nullable = false)
    val createdAt: Long = System.currentTimeMillis(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = System.currentTimeMillis(),
) : Persistable<UUID> {
    override fun getId(): UUID = id

    @Transient
    private var newEntity: Boolean = true

    override fun isNew(): Boolean = newEntity

    @PostLoad
    @PostPersist
    private fun markNotNew() {
        newEntity = false
    }

    fun getRoleSet(): Set<UserRole> =
        roles.split(",").mapNotNull { runCatching { UserRole.valueOf(it.trim()) }.getOrNull() }.toSet()

    fun hasRole(role: UserRole): Boolean = getRoleSet().contains(role)
}

enum class OnboardingStatus { NOT_STARTED, IN_PROGRESS, COMPLETED }

enum class UserRole { USER, ADMIN, THERAPIST }

enum class AuthProvider { PASSWORD, GOOGLE }
