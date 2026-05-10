package com.tranquilai.plan.controller

import com.tranquilai.plan.dto.request.CompleteActivityRequest
import com.tranquilai.plan.dto.request.GeneratePlanRequest
import com.tranquilai.plan.dto.response.DailyPlanResponse
import com.tranquilai.plan.security.GatewayUser
import com.tranquilai.plan.service.PlanService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/plans")
class PlanController(private val planService: PlanService) {

    /** POST /api/plans/today — get today's plan or generate one via AI */
    @PostMapping("/today")
    fun getOrGenerateToday(
        @AuthenticationPrincipal user: GatewayUser,
        @RequestBody(required = false) request: GeneratePlanRequest?,
    ): ResponseEntity<DailyPlanResponse> =
        ResponseEntity.ok(planService.getOrGenerate(user.id, request ?: GeneratePlanRequest()))

    /** GET /api/plans — list all plans for user (most recent first) */
    @GetMapping
    fun list(@AuthenticationPrincipal user: GatewayUser): ResponseEntity<List<DailyPlanResponse>> =
        ResponseEntity.ok(planService.list(user.id))

    /** GET /api/plans/{id} */
    @GetMapping("/{id}")
    fun get(
        @AuthenticationPrincipal user: GatewayUser,
        @PathVariable id: UUID,
    ): ResponseEntity<DailyPlanResponse> =
        ResponseEntity.ok(planService.get(user.id, id))

    /** PATCH /api/plans/{planId}/activities/{activityId}/complete */
    @PatchMapping("/{planId}/activities/{activityId}/complete")
    fun completeActivity(
        @AuthenticationPrincipal user: GatewayUser,
        @PathVariable planId: UUID,
        @PathVariable activityId: UUID,
        @RequestBody(required = false) request: CompleteActivityRequest?,
    ): ResponseEntity<DailyPlanResponse> =
        ResponseEntity.ok(planService.completeActivity(user.id, planId, activityId, request ?: CompleteActivityRequest()))
}
