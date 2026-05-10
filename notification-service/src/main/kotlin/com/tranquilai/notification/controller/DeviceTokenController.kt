package com.tranquilai.notification.controller

import com.tranquilai.notification.dto.request.RegisterDeviceTokenRequest
import com.tranquilai.notification.dto.response.DeviceTokenResponse
import com.tranquilai.notification.security.GatewayUser
import com.tranquilai.notification.service.DeviceTokenService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/notifications/device-tokens")
class DeviceTokenController(private val service: DeviceTokenService) {

    /**
     * POST /api/notifications/device-tokens
     * Called on app launch / after login to register the FCM token.
     */
    @PostMapping
    fun register(
        @AuthenticationPrincipal user: GatewayUser,
        @Valid @RequestBody request: RegisterDeviceTokenRequest,
    ): ResponseEntity<DeviceTokenResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.register(user.id, request))

    /**
     * GET /api/notifications/device-tokens
     * Returns all active tokens for the current user.
     */
    @GetMapping
    fun list(
        @AuthenticationPrincipal user: GatewayUser,
    ): ResponseEntity<List<DeviceTokenResponse>> =
        ResponseEntity.ok(service.listForUser(user.id))

    /**
     * DELETE /api/notifications/device-tokens/{token}
     * Called on logout or when the app detects a stale token.
     */
    @DeleteMapping("/{token}")
    fun deactivate(
        @AuthenticationPrincipal user: GatewayUser,
        @PathVariable token: String,
    ): ResponseEntity<Void> {
        service.deactivate(user.id, token)
        return ResponseEntity.noContent().build()
    }
}
