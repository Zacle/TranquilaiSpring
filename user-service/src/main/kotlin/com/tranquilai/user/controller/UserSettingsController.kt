package com.tranquilai.user.controller

import com.tranquilai.user.dto.request.UpdateUserSettingsRequest
import com.tranquilai.user.dto.response.UserSettingsResponse
import com.tranquilai.user.security.GatewayUser
import com.tranquilai.user.service.UserSettingsService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users/me/settings")
class UserSettingsController(private val settingsService: UserSettingsService) {

    /** GET /api/users/me/settings */
    @GetMapping
    fun getSettings(
        @AuthenticationPrincipal user: GatewayUser,
    ): ResponseEntity<UserSettingsResponse> =
        ResponseEntity.ok(settingsService.getSettings(user.id))

    /** PUT /api/users/me/settings — partial update (all fields optional) */
    @PutMapping
    fun updateSettings(
        @AuthenticationPrincipal user: GatewayUser,
        @RequestBody request: UpdateUserSettingsRequest,
    ): ResponseEntity<UserSettingsResponse> =
        ResponseEntity.ok(settingsService.updateSettings(user.id, request))
}
