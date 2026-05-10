package com.tranquilai.user.controller

import com.tranquilai.user.dto.request.SaveMentalHealthProfileRequest
import com.tranquilai.user.dto.response.MentalHealthProfileResponse
import com.tranquilai.user.security.GatewayUser
import com.tranquilai.user.service.MentalHealthProfileService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users/me/mental-health-profile")
class MentalHealthProfileController(
    private val profileService: MentalHealthProfileService,
) {
    /** GET /api/users/me/mental-health-profile */
    @GetMapping
    fun getProfile(
        @AuthenticationPrincipal user: GatewayUser,
    ): ResponseEntity<MentalHealthProfileResponse> {
        val profile = profileService.getProfile(user.id)
        return if (profile != null) ResponseEntity.ok(profile)
        else ResponseEntity.noContent().build()
    }

    /** PUT /api/users/me/mental-health-profile — create or update */
    @PutMapping
    fun saveProfile(
        @AuthenticationPrincipal user: GatewayUser,
        @RequestBody request: SaveMentalHealthProfileRequest,
    ): ResponseEntity<MentalHealthProfileResponse> =
        ResponseEntity.ok(profileService.saveProfile(user.id, request))
}
