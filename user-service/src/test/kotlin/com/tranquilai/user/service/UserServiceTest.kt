package com.tranquilai.user.service

import com.tranquilai.user.client.AiServiceClient
import com.tranquilai.user.client.AuthServiceClient
import com.tranquilai.user.client.NotificationServiceClient
import com.tranquilai.user.client.SubscriptionServiceClient
import com.tranquilai.user.dto.request.CreateUserRequest
import com.tranquilai.user.dto.request.UpdateOnboardingStatusRequest
import com.tranquilai.user.dto.request.UpdateUserRequest
import com.tranquilai.user.entity.OnboardingStatus
import com.tranquilai.user.entity.User
import com.tranquilai.user.exception.UserNotFoundException
import com.tranquilai.user.repository.EmergencyContactRepository
import com.tranquilai.user.repository.MentalHealthProfileRepository
import com.tranquilai.user.repository.UserRepository
import com.tranquilai.user.repository.UserSettingsRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.web.multipart.MultipartFile
import java.util.Optional
import java.util.UUID

class UserServiceTest {

    private val userRepository: UserRepository = mock(UserRepository::class.java)
    private val authServiceClient: AuthServiceClient = mock(AuthServiceClient::class.java)
    private val aiServiceClient: AiServiceClient = mock(AiServiceClient::class.java)
    private val subscriptionServiceClient: SubscriptionServiceClient = mock(SubscriptionServiceClient::class.java)
    private val notificationServiceClient: NotificationServiceClient = mock(NotificationServiceClient::class.java)
    private val emergencyContactRepository: EmergencyContactRepository = mock(EmergencyContactRepository::class.java)
    private val mentalHealthProfileRepository: MentalHealthProfileRepository = mock(MentalHealthProfileRepository::class.java)
    private val userSettingsRepository: UserSettingsRepository = mock(UserSettingsRepository::class.java)
    private val profilePictureStorageService: ProfilePictureStorageService = mock(ProfilePictureStorageService::class.java)
    private val service = UserService(
        userRepository,
        authServiceClient,
        aiServiceClient,
        subscriptionServiceClient,
        notificationServiceClient,
        emergencyContactRepository,
        mentalHealthProfileRepository,
        userSettingsRepository,
        profilePictureStorageService,
    )

    @Test
    fun `createUser saves internal registration payload`() {
        val id = UUID.randomUUID()
        val request = CreateUserRequest(
            id = id,
            email = "user@example.com",
            username = "user",
            firstName = "Test",
            lastName = "User",
        )
        `when`(userRepository.findById(id)).thenReturn(Optional.empty())
        `when`(userRepository.save(anyEntity())).thenAnswer { it.getArgument<User>(0) }

        val response = service.createUser(request)

        assertEquals(id, response.id)
        assertEquals("user@example.com", response.email)
        assertEquals("Test User", response.fullName)
        verify(userRepository).save(anyEntity())
    }

    @Test
    fun `createUser returns existing user when event is delivered again`() {
        val existing = testUser()
        val request = CreateUserRequest(
            id = existing.id,
            email = existing.email,
            username = existing.username,
            firstName = existing.firstName,
            lastName = existing.lastName,
        )
        `when`(userRepository.findById(existing.id)).thenReturn(Optional.of(existing))

        val response = service.createUser(request)

        assertEquals(existing.id, response.id)
        verify(userRepository, never()).save(anyEntity())
    }

    @Test
    fun `getUser returns user response`() {
        val user = testUser()
        `when`(userRepository.findById(user.id)).thenReturn(Optional.of(user))

        val response = service.getUser(user.id)

        assertEquals(user.id, response.id)
        assertEquals("Test User", response.fullName)
    }

    @Test
    fun `getUser throws when user is missing`() {
        val id = UUID.randomUUID()
        `when`(userRepository.findById(id)).thenReturn(Optional.empty())

        assertThrows(UserNotFoundException::class.java) {
            service.getUser(id)
        }
    }

    @Test
    fun `updateUser applies only provided fields`() {
        val user = testUser(firstName = "Old", lastName = "Name")
        `when`(userRepository.findById(user.id)).thenReturn(Optional.of(user))
        `when`(userRepository.save(user)).thenReturn(user)

        val response = service.updateUser(
            user.id,
            UpdateUserRequest(
                firstName = "New",
                phoneNumber = "+15551234567",
                timezone = "Africa/Cairo",
                languagePreference = "ar",
            ),
        )

        assertEquals("New", response.firstName)
        assertEquals("Name", response.lastName)
        assertEquals("+15551234567", response.phoneNumber)
        assertEquals("Africa/Cairo", response.timezone)
        assertEquals("ar", response.languagePreference)
        verify(userRepository).save(user)
    }

    @Test
    fun `updateOnboardingStatus saves new status`() {
        val user = testUser()
        `when`(userRepository.findById(user.id)).thenReturn(Optional.of(user))
        `when`(userRepository.save(user)).thenReturn(user)

        val response = service.updateOnboardingStatus(
            user.id,
            UpdateOnboardingStatusRequest(OnboardingStatus.COMPLETED),
        )

        assertEquals(OnboardingStatus.COMPLETED, response.onboardingStatus)
        verify(userRepository).save(user)
    }

    @Test
    fun `updateProfilePicture uploads file and saves returned url`() {
        val user = testUser()
        val file = mock(MultipartFile::class.java)
        `when`(userRepository.findById(user.id)).thenReturn(Optional.of(user))
        `when`(profilePictureStorageService.uploadProfilePicture(user.id, file))
            .thenReturn("https://firebasestorage.googleapis.com/profile.jpg")
        `when`(userRepository.save(user)).thenReturn(user)

        val response = service.updateProfilePicture(user.id, file)

        assertEquals("https://firebasestorage.googleapis.com/profile.jpg", response.profilePictureUrl)
        verify(profilePictureStorageService).uploadProfilePicture(user.id, file)
        verify(userRepository).save(user)
    }

    @Test
    fun `deactivateUser marks local user inactive and notifies auth-service`() {
        val user = testUser()
        `when`(userRepository.findById(user.id)).thenReturn(Optional.of(user))
        `when`(userRepository.save(user)).thenReturn(user)

        service.deactivateUser(user.id)

        assertFalse(user.isActive)
        verify(userRepository).save(user)
        verify(authServiceClient).deactivateUser(user.id)
    }

    @Test
    fun `deleteUser removes account data across services and revokes auth user`() {
        val id = UUID.randomUUID()
        `when`(userRepository.existsById(id)).thenReturn(true)

        service.deleteUser(id)

        verify(subscriptionServiceClient).deleteSubscriptionData(id)
        verify(aiServiceClient).deleteChatHistory(id)
        verify(notificationServiceClient).deleteUserData(id)
        verify(profilePictureStorageService).deleteProfilePictures(id)
        verify(emergencyContactRepository).deleteByUserId(id)
        verify(userSettingsRepository).deleteByUserId(id)
        verify(mentalHealthProfileRepository).deleteByUserId(id)
        verify(userRepository).deleteById(id)
        verify(authServiceClient).deleteUserOrThrow(id)
    }

    @Test
    fun `deleteUser throws and does not delete related data when missing`() {
        val id = UUID.randomUUID()
        `when`(userRepository.existsById(id)).thenReturn(false)

        assertThrows(UserNotFoundException::class.java) {
            service.deleteUser(id)
        }

        verify(userRepository, never()).deleteById(id)
        verify(subscriptionServiceClient, never()).deleteSubscriptionData(id)
        verify(aiServiceClient, never()).deleteChatHistory(id)
        verify(notificationServiceClient, never()).deleteUserData(id)
        verify(profilePictureStorageService, never()).deleteProfilePictures(id)
        verify(authServiceClient, never()).deleteUserOrThrow(id)
    }

    private fun testUser(
        id: UUID = UUID.randomUUID(),
        firstName: String = "Test",
        lastName: String = "User",
    ) = User(
        id = id,
        email = "user@example.com",
        username = "user",
        firstName = firstName,
        lastName = lastName,
    )

    @Suppress("UNCHECKED_CAST")
    private fun anyEntity(): User {
        any(User::class.java)
        return uninitialized()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> uninitialized(): T = null as T
}
