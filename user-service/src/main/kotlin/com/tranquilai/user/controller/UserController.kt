package com.tranquilai.user.controller

import com.tranquilai.user.dto.request.UpdateOnboardingStatusRequest
import com.tranquilai.user.dto.request.UpdateUserRequest
import com.tranquilai.user.dto.response.UserResponse
import com.tranquilai.user.security.GatewayUser
import com.tranquilai.user.service.UserService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
) {
    /** GET /api/users/me — current user's profile */
    @GetMapping("/me")
    fun getMe(@AuthenticationPrincipal user: GatewayUser): ResponseEntity<UserResponse> =
        ResponseEntity.ok(userService.getUser(user.id))

    /** PUT /api/users/me — update profile fields */
    @PutMapping("/me")
    fun updateMe(
        @AuthenticationPrincipal user: GatewayUser,
        @RequestBody request: UpdateUserRequest,
    ): ResponseEntity<UserResponse> =
        ResponseEntity.ok(userService.updateUser(user.id, request))

    /** POST /api/users/me/profile-picture — upload and update profile picture */
    @PostMapping("/me/profile-picture", consumes = ["multipart/form-data"])
    fun updateProfilePicture(
        @AuthenticationPrincipal user: GatewayUser,
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<UserResponse> =
        ResponseEntity.ok(userService.updateProfilePicture(user.id, file))

    /** PATCH /api/users/me/onboarding-status — update onboarding status */
    @PatchMapping("/me/onboarding-status")
    fun updateOnboardingStatus(
        @AuthenticationPrincipal user: GatewayUser,
        @Valid @RequestBody request: UpdateOnboardingStatusRequest,
    ): ResponseEntity<UserResponse> =
        ResponseEntity.ok(userService.updateOnboardingStatus(user.id, request))

    /** DELETE /api/users/me — soft-delete (deactivate) account */
    @DeleteMapping("/me")
    fun deactivateMe(@AuthenticationPrincipal user: GatewayUser): ResponseEntity<Void> {
        userService.deactivateUser(user.id)
        return ResponseEntity.noContent().build()
    }

    /** DELETE /api/users/me/account — permanent hard delete, removes all user data */
    @DeleteMapping("/me/account")
    fun deleteMe(@AuthenticationPrincipal user: GatewayUser): ResponseEntity<Void> {
        userService.deleteUser(user.id)
        return ResponseEntity.noContent().build()
    }
}
