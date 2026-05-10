package com.tranquilai.user.repository

import com.tranquilai.user.entity.EmergencyContact
import com.tranquilai.user.entity.MentalHealthProfile
import com.tranquilai.user.entity.User
import com.tranquilai.user.entity.UserSettings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource
import java.util.UUID

@DataJpaTest
@TestPropertySource(
    properties = [
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
    ],
)
class UserRepositoryIntegrationTest @Autowired constructor(
    private val userRepository: UserRepository,
    private val settingsRepository: UserSettingsRepository,
    private val profileRepository: MentalHealthProfileRepository,
    private val emergencyContactRepository: EmergencyContactRepository,
    private val jdbcTemplate: JdbcTemplate,
) {

    @Test
    fun `user repository finds by email and existence checks`() {
        val user = insertUser(email = "find@example.com", username = "find_user")

        assertEquals(user.id, userRepository.findByEmail("find@example.com").get().id)
        assertTrue(userRepository.existsByEmail("find@example.com"))
        assertTrue(userRepository.existsByUsername("find_user"))
        assertFalse(userRepository.existsByEmail("missing@example.com"))
    }

    @Test
    fun `settings repository finds settings by user id`() {
        val user = insertUser(email = "settings@example.com", username = "settings_user")
        val settings = insertSettings(user)

        val found = settingsRepository.findByUserId(user.id)

        assertTrue(found.isPresent)
        assertEquals(settings.id, found.get().id)
    }

    @Test
    fun `profile repository finds and checks profile by user id`() {
        val user = insertUser(email = "profile@example.com", username = "profile_user")
        val profile = insertProfile(user)

        assertTrue(profileRepository.existsByUserId(user.id))
        assertEquals(profile.id, profileRepository.findByUserId(user.id).get().id)
    }

    @Test
    fun `emergency contact repository supports primary ordering and delete by user`() {
        val user = insertUser(email = "contacts@example.com", username = "contacts_user")
        val otherUser = insertUser(email = "other@example.com", username = "other_user")
        val older = insertContact(user.id, name = "Older", isPrimary = true, createdAt = 100)
        val newer = insertContact(user.id, name = "Newer", isPrimary = false, createdAt = 200)
        insertContact(otherUser.id, name = "Other", isPrimary = true, createdAt = 300)

        val ordered = emergencyContactRepository.findByUserIdOrderByCreatedAtDesc(user.id)
        assertEquals(listOf(newer.id, older.id), ordered.map { it.id })
        assertEquals(older.id, emergencyContactRepository.findByUserIdAndIsPrimaryTrue(user.id)?.id)
        assertEquals(newer.id, emergencyContactRepository.findByIdAndUserId(newer.id, user.id)?.id)

        emergencyContactRepository.clearPrimaryForUser(user.id)
        assertEquals(null, emergencyContactRepository.findByUserIdAndIsPrimaryTrue(user.id))
        assertEquals(1, emergencyContactRepository.findByUserIdOrderByCreatedAtDesc(otherUser.id).size)

        emergencyContactRepository.deleteByUserId(user.id)
        assertTrue(emergencyContactRepository.findByUserIdOrderByCreatedAtDesc(user.id).isEmpty())
        assertEquals(1, emergencyContactRepository.findByUserIdOrderByCreatedAtDesc(otherUser.id).size)
    }

    private fun insertUser(
        email: String,
        username: String,
        id: UUID = UUID.randomUUID(),
    ): User {
        val user = User(
            id = id,
            email = email,
            username = username,
            firstName = "Test",
            lastName = "User",
        )
        jdbcTemplate.update(
            """
            INSERT INTO users (
                id, email, username, first_name, last_name, language_preference,
                onboarding_status, is_active, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            user.id,
            user.email,
            user.username,
            user.firstName,
            user.lastName,
            user.languagePreference,
            user.onboardingStatus.name,
            user.isActive,
            user.createdAt,
            user.updatedAt,
        )
        return user
    }

    private fun insertSettings(user: User): UserSettings {
        val settings = UserSettings(user = user)
        jdbcTemplate.update(
            """
            INSERT INTO user_settings (
                id, user_id, theme_preference, notifications_enabled, reminder_enabled,
                reminder_times, reminder_frequency, preferred_content_language,
                show_explicit_content, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            settings.id,
            user.id,
            settings.themePreference,
            settings.notificationsEnabled,
            settings.reminderEnabled,
            settings.reminderTimes,
            settings.reminderFrequency,
            settings.preferredContentLanguage,
            settings.showExplicitContent,
            settings.createdAt,
            settings.updatedAt,
        )
        return settings
    }

    private fun insertProfile(user: User): MentalHealthProfile {
        val profile = MentalHealthProfile(user = user)
        jdbcTemplate.update(
            """
            INSERT INTO mental_health_profiles (
                id, user_id, urgency_level, support_intensity, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            profile.id,
            user.id,
            profile.urgencyLevel.name,
            profile.supportIntensity.name,
            profile.createdAt,
            profile.updatedAt,
        )
        return profile
    }

    private fun insertContact(
        userId: UUID,
        name: String,
        isPrimary: Boolean,
        createdAt: Long,
    ): EmergencyContact {
        val contact = EmergencyContact(
            userId = userId,
            name = name,
            phoneNumber = "+15550001111",
            relationship = "Friend",
            isPrimary = isPrimary,
            createdAt = createdAt,
            updatedAt = createdAt,
        )
        jdbcTemplate.update(
            """
            INSERT INTO emergency_contacts (
                id, user_id, name, phone_number, email, relationship,
                is_primary, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            contact.id,
            contact.userId,
            contact.name,
            contact.phoneNumber,
            contact.email,
            contact.relationship,
            contact.isPrimary,
            contact.createdAt,
            contact.updatedAt,
        )
        return contact
    }
}
