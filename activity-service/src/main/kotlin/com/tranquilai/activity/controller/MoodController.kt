package com.tranquilai.activity.controller

import com.tranquilai.activity.dto.request.LogMoodRequest
import com.tranquilai.activity.dto.request.UpdateMoodInsightRequest
import com.tranquilai.activity.dto.request.UpdateMoodRequest
import com.tranquilai.activity.dto.response.MoodEntryResponse
import com.tranquilai.activity.dto.response.PageResponse
import com.tranquilai.activity.security.GatewayUser
import com.tranquilai.activity.service.MoodService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/mood")
class MoodController(private val moodService: MoodService) {

    /** POST /api/mood — log a mood entry */
    @PostMapping
    fun log(
        @AuthenticationPrincipal user: GatewayUser,
        @Valid @RequestBody request: LogMoodRequest,
    ): ResponseEntity<MoodEntryResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(moodService.log(user.id, request))

    /** GET /api/mood — paginated mood history */
    @GetMapping
    fun list(
        @AuthenticationPrincipal user: GatewayUser,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<PageResponse<MoodEntryResponse>> =
        ResponseEntity.ok(moodService.list(user.id, page, size))

    /** GET /api/mood/{id} — single entry */
    @GetMapping("/{id}")
    fun get(
        @AuthenticationPrincipal user: GatewayUser,
        @PathVariable id: UUID,
    ): ResponseEntity<MoodEntryResponse> =
        ResponseEntity.ok(moodService.get(user.id, id))

    /** GET /api/mood/history?from=&to= — date-range history */
    @GetMapping("/history")
    fun history(
        @AuthenticationPrincipal user: GatewayUser,
        @RequestParam from: Long,
        @RequestParam to: Long,
    ): ResponseEntity<List<MoodEntryResponse>> =
        ResponseEntity.ok(moodService.history(user.id, from, to))

    /** GET /api/mood/today — today's most recent mood entry */
    @GetMapping("/today")
    fun today(
        @AuthenticationPrincipal user: GatewayUser,
    ): ResponseEntity<MoodEntryResponse> {
        val entry = moodService.today(user.id)
            ?: return ResponseEntity.noContent().build()
        return ResponseEntity.ok(entry)
    }

    /** PUT /api/mood/{id} — update a mood entry */
    @PutMapping("/{id}")
    fun update(
        @AuthenticationPrincipal user: GatewayUser,
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateMoodRequest,
    ): ResponseEntity<MoodEntryResponse> =
        ResponseEntity.ok(moodService.update(user.id, id, request))

    /** PATCH /api/mood/{id}/insight — update AI insight */
    @PatchMapping("/{id}/insight")
    fun updateInsight(
        @AuthenticationPrincipal user: GatewayUser,
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateMoodInsightRequest,
    ): ResponseEntity<MoodEntryResponse> =
        ResponseEntity.ok(moodService.updateInsight(user.id, id, request))

    /** DELETE /api/mood/{id} */
    @DeleteMapping("/{id}")
    fun delete(
        @AuthenticationPrincipal user: GatewayUser,
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        moodService.delete(user.id, id)
        return ResponseEntity.noContent().build()
    }
}
