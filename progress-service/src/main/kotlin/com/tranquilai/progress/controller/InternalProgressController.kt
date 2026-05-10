package com.tranquilai.progress.controller

import com.tranquilai.progress.dto.request.UpdateStatsRequest
import com.tranquilai.progress.dto.response.UserStatsResponse
import com.tranquilai.progress.service.ProgressService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/internal/progress")
@PreAuthorize("hasRole('INTERNAL')")
class InternalProgressController(private val progressService: ProgressService) {

    /** PATCH /internal/progress/stats/{userId} — increment counters called by activity-service */
    @PatchMapping("/stats/{userId}")
    fun updateStats(
        @PathVariable userId: UUID,
        @RequestBody request: UpdateStatsRequest,
    ): ResponseEntity<UserStatsResponse> =
        ResponseEntity.ok(progressService.updateStats(userId, request))
}
