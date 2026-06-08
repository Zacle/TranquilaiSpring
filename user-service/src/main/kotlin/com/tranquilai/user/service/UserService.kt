package com.tranquilai.user.service

import com.tranquilai.user.client.AiServiceClient
import com.tranquilai.user.client.AuthServiceClient
import com.tranquilai.user.client.NotificationServiceClient
import com.tranquilai.user.client.SubscriptionServiceClient
import com.tranquilai.user.dto.request.CreateUserRequest
import com.tranquilai.user.dto.request.UpdateOnboardingStatusRequest
import com.tranquilai.user.dto.request.UpdateUserRequest
import com.tranquilai.user.dto.response.UserResponse
import com.tranquilai.user.entity.User
import com.tranquilai.user.exception.UserNotFoundException
import com.tranquilai.user.repository.EmergencyContactRepository
import com.tranquilai.user.repository.MentalHealthProfileRepository
import com.tranquilai.user.repository.UserRepository
import com.tranquilai.user.repository.UserSettingsRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val authServiceClient: AuthServiceClient,
    private val aiServiceClient: AiServiceClient,
    private val subscriptionServiceClient: SubscriptionServiceClient,
    private val notificationServiceClient: NotificationServiceClient,
    private val emergencyContactRepository: EmergencyContactRepository,
    private val mentalHealthProfileRepository: MentalHealthProfileRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val profilePictureStorageService: ProfilePictureStorageService,
) {
    /** Called by auth-service via internal API after registration */
    fun createUser(request: CreateUserRequest): UserResponse {
        userRepository.findById(request.id).orElse(null)?.let { existing ->
            return existing.toResponse()
        }

        val user = User(
            id = request.id,
            email = request.email,
            username = request.username,
            firstName = request.firstName,
            lastName = request.lastName,
        )
        return userRepository.save(user).toResponse()
    }

    @Transactional(readOnly = true)
    fun getUser(userId: UUID): UserResponse =
        userRepository.findById(userId)
            .orElseThrow { UserNotFoundException("User $userId not found") }
            .toResponse()

    fun updateUser(userId: UUID, request: UpdateUserRequest): UserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException("User $userId not found") }

        request.firstName?.let { user.firstName = it }
        request.lastName?.let { user.lastName = it }
        request.dateOfBirth?.let { user.dateOfBirth = it }
        request.phoneNumber?.let { user.phoneNumber = it }
        request.timezone?.let { user.timezone = it }
        request.languagePreference?.let { user.languagePreference = it }
        request.profilePictureUrl?.let { user.profilePictureUrl = it }
        user.updatedAt = System.currentTimeMillis()

        return userRepository.save(user).toResponse()
    }

    fun updateProfilePicture(
        userId: UUID,
        file: MultipartFile,
    ): UserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException("User $userId not found") }

        user.profilePictureUrl = profilePictureStorageService.uploadProfilePicture(userId, file)
        user.updatedAt = System.currentTimeMillis()

        return userRepository.save(user).toResponse()
    }

    fun updateOnboardingStatus(userId: UUID, request: UpdateOnboardingStatusRequest): UserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException("User $userId not found") }

        user.onboardingStatus = request.status
        user.updatedAt = System.currentTimeMillis()

        return userRepository.save(user).toResponse()
    }

    fun deactivateUser(userId: UUID) {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException("User $userId not found") }

        user.isActive = false
        user.updatedAt = System.currentTimeMillis()
        userRepository.save(user)
        authServiceClient.deactivateUser(userId)
    }

    fun deleteUser(userId: UUID) {
        if (!userRepository.existsById(userId)) throw UserNotFoundException("User $userId not found")
        subscriptionServiceClient.deleteSubscriptionData(userId)
        aiServiceClient.deleteChatHistory(userId)
        notificationServiceClient.deleteUserData(userId)
        profilePictureStorageService.deleteProfilePictures(userId)
        emergencyContactRepository.deleteByUserId(userId)
        userSettingsRepository.deleteByUserId(userId)
        mentalHealthProfileRepository.deleteByUserId(userId)
        userRepository.deleteById(userId)
        authServiceClient.deleteUserOrThrow(userId)
    }
}

fun User.toResponse() = UserResponse(
    id = id,
    email = email,
    username = username,
    firstName = firstName,
    lastName = lastName,
    fullName = fullName,
    dateOfBirth = dateOfBirth,
    phoneNumber = phoneNumber,
    timezone = timezone,
    languagePreference = languagePreference,
    onboardingStatus = onboardingStatus,
    profilePictureUrl = profilePictureUrl,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
