package com.tranquilai.activity.controller

import com.tranquilai.activity.dto.request.LogBreathingSessionRequest
import com.tranquilai.activity.dto.response.BreathingSessionResponse
import com.tranquilai.activity.dto.response.PageResponse
import com.tranquilai.activity.security.GatewayUser
import com.tranquilai.activity.service.BreathingService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/breathing-sessions")
class BreathingController(private val breathingService: BreathingService) {

    /** POST /api/breathing-sessions — log a session */
    @PostMapping
    fun log(
        @AuthenticationPrincipal user: GatewayUser,
        @Valid @RequestBody request: LogBreathingSessionRequest,
    ): ResponseEntity<BreathingSessionResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(breathingService.log(user.id, request))

    /** GET /api/breathing-sessions */
    @GetMapping
    fun list(
        @AuthenticationPrincipal user: GatewayUser,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<PageResponse<BreathingSessionResponse>> =
        ResponseEntity.ok(breathingService.list(user.id, page, size))

    /** GET /api/breathing-sessions/history?from=&to= — sessions in epoch-ms date range */
    @GetMapping("/history")
    fun history(
        @AuthenticationPrincipal user: GatewayUser,
        @RequestParam from: Long,
        @RequestParam to: Long,
    ): ResponseEntity<List<BreathingSessionResponse>> =
        ResponseEntity.ok(breathingService.history(user.id, from, to))

    /** GET /api/breathing-sessions/stats */
    @GetMapping("/stats")
    fun stats(@AuthenticationPrincipal user: GatewayUser): ResponseEntity<Map<String, Long>> =
        ResponseEntity.ok(mapOf("completedCount" to breathingService.completedCount(user.id)))
}
