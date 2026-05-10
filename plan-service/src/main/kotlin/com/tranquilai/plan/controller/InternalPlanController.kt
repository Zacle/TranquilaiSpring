package com.tranquilai.plan.controller

import com.tranquilai.plan.service.PlanService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/internal/plans")
@PreAuthorize("hasRole('INTERNAL')")
class InternalPlanController(private val planService: PlanService) {

    /**
     * POST /internal/plans/{userId}/complete-by-type?type=MOOD_TRACKING
     * Marks the first uncompleted activity of the given type in today's plan as completed.
     * Called by activity-service after each activity is logged.
     */
    @PostMapping("/{userId}/complete-by-type")
    fun completeByType(
        @PathVariable userId: UUID,
        @RequestParam type: String,
    ): ResponseEntity<Map<String, Boolean>> {
        val completed = planService.completeByType(userId, type)
        return ResponseEntity.ok(mapOf("completed" to completed))
    }
}
