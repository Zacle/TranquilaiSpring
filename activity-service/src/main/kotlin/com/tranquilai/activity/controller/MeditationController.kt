package com.tranquilai.activity.controller

import com.tranquilai.activity.dto.request.LogMeditationSessionRequest
import com.tranquilai.activity.dto.response.MeditationSessionResponse
import com.tranquilai.activity.dto.response.PageResponse
import com.tranquilai.activity.security.GatewayUser
import com.tranquilai.activity.service.MeditationService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/meditation-sessions")
class MeditationController(private val meditationService: MeditationService) {

    /** POST /api/meditation-sessions — log a session */
    @PostMapping
    fun log(
        @AuthenticationPrincipal user: GatewayUser,
        @Valid @RequestBody request: LogMeditationSessionRequest,
    ): ResponseEntity<MeditationSessionResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(meditationService.log(user.id, request))

    /** GET /api/meditation-sessions */
    @GetMapping
    fun list(
        @AuthenticationPrincipal user: GatewayUser,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<PageResponse<MeditationSessionResponse>> =
        ResponseEntity.ok(meditationService.list(user.id, page, size))

    /** GET /api/meditation-sessions/history?from=&to= — sessions in epoch-ms date range */
    @GetMapping("/history")
    fun history(
        @AuthenticationPrincipal user: GatewayUser,
        @RequestParam from: Long,
        @RequestParam to: Long,
    ): ResponseEntity<List<MeditationSessionResponse>> =
        ResponseEntity.ok(meditationService.history(user.id, from, to))

    /** GET /api/meditation-sessions/stats */
    @GetMapping("/stats")
    fun stats(@AuthenticationPrincipal user: GatewayUser): ResponseEntity<Map<String, Long>> =
        ResponseEntity.ok(mapOf("completedCount" to meditationService.completedCount(user.id)))
}
