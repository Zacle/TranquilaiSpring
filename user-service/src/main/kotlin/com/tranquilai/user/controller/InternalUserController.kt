package com.tranquilai.user.controller

import com.tranquilai.user.dto.request.CreateUserRequest
import com.tranquilai.user.dto.response.PlanContextResponse
import com.tranquilai.user.dto.response.UserResponse
import com.tranquilai.user.service.MentalHealthProfileService
import com.tranquilai.user.service.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * Internal controller for service-to-service communication.
 * Protected by X-Internal-Key header — not routed through the public API gateway.
 */
@RestController
@RequestMapping("/internal/users")
@PreAuthorize("hasRole('INTERNAL')")
class InternalUserController(
    private val userService: UserService,
    private val profileService: MentalHealthProfileService,
) {
    /** Called by auth-service after successful registration */
    @PostMapping
    fun createUser(@Valid @RequestBody request: CreateUserRequest): ResponseEntity<UserResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request))

    /** Called by other services to look up user details */
    @GetMapping("/{userId}")
    fun getUserById(@PathVariable userId: UUID): ResponseEntity<UserResponse> =
        ResponseEntity.ok(userService.getUser(userId))

    /**
     * Called by plan-service to get user context for AI plan personalisation.
     * Returns user first name + mental health profile fields.
     */
    @GetMapping("/{userId}/plan-context")
    fun getPlanContext(@PathVariable userId: UUID): ResponseEntity<PlanContextResponse> {
        val user = userService.getUser(userId)
        val profile = profileService.getProfile(userId)
        return ResponseEntity.ok(
            PlanContextResponse(
                userId = user.id,
                firstName = user.firstName,
                currentFeelingLevel = profile?.currentFeelingLevel,
                stressCauses = profile?.stressCauses ?: emptyList(),
                currentConcerns = profile?.currentConcerns ?: emptyList(),
                mentalProcessPreferences = profile?.mentalProcessPreferences ?: emptyList(),
                personalGoals = profile?.personalGoals ?: emptyList(),
                urgencyLevel = profile?.urgencyLevel?.name ?: "LOW",
                supportIntensity = profile?.supportIntensity?.name ?: "LIGHT",
                communicationStyle = profile?.communicationStyle?.name,
                baselineAnxietyLevel = profile?.baselineAnxietyLevel,
                baselineStressLevel = profile?.baselineStressLevel,
                baselineWellbeingLevel = profile?.baselineWellbeingLevel,
                recommendedApproach = profile?.recommendedApproach,
            )
        )
    }
}
