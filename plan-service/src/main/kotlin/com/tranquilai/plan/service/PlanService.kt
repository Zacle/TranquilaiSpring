package com.tranquilai.plan.service

import com.tranquilai.plan.client.ProgressServiceClient
import com.tranquilai.plan.client.UserServiceClient
import com.tranquilai.plan.dto.request.CompleteActivityRequest
import com.tranquilai.plan.dto.request.GeneratePlanRequest
import com.tranquilai.plan.dto.response.DailyPlanResponse
import com.tranquilai.plan.dto.response.PlanActivityResponse
import com.tranquilai.plan.entity.DailyPlan
import com.tranquilai.plan.entity.PlanActivity
import com.tranquilai.plan.exception.ResourceNotFoundException
import com.tranquilai.plan.repository.DailyPlanRepository
import com.tranquilai.plan.repository.PlanActivityRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

@Service
@Transactional
class PlanService(
    private val planRepo: DailyPlanRepository,
    private val activityRepo: PlanActivityRepository,
    private val planGenerator: PlanGeneratorService,
    private val userServiceClient: UserServiceClient,
    private val progressServiceClient: ProgressServiceClient,
) {
    /** Returns today's plan if one exists, otherwise generates a new one via AI */
    fun getOrGenerate(userId: UUID, request: GeneratePlanRequest): DailyPlanResponse {
        val todayStart = todayStartEpoch()

        if (!request.forceRegenerate) {
            val existing = planRepo.findByUserIdAndPlanDate(userId, todayStart)
            if (existing.isPresent) {
                return planRepo.findByIdWithActivities(existing.get().id).get().toResponse()
            }
        }

        return generate(userId, request, todayStart)
    }

    private fun generate(userId: UUID, request: GeneratePlanRequest, planDate: Long): DailyPlanResponse {
        // Fetch user's mental health profile for personalisation
        val context = userServiceClient.getPlanContext(userId)

        val aiPlan = planGenerator.generate(context, request.languageCode)
        val totalMinutes = aiPlan.activities.sumOf { it.durationMinutes }

        val plan = planRepo.save(
            DailyPlan(
                userId = userId,
                planDate = planDate,
                greeting = aiPlan.greeting,
                motivationalMessage = aiPlan.motivationalMessage,
                totalDurationMinutes = totalMinutes,
            )
        )

        aiPlan.activities.forEachIndexed { index, act ->
            activityRepo.save(
                PlanActivity(
                    plan = plan,
                    activityType = act.type,
                    title = act.title,
                    description = act.description,
                    prompt = act.prompt,
                    durationMinutes = act.durationMinutes,
                    sortOrder = index,
                )
            )
        }

        return planRepo.findByIdWithActivities(plan.id).get().toResponse()
    }

    @Transactional(readOnly = true)
    fun list(userId: UUID): List<DailyPlanResponse> =
        planRepo.findByUserIdOrderByPlanDateDesc(userId)
            .map { planRepo.findByIdWithActivities(it.id).get().toResponse() }

    @Transactional(readOnly = true)
    fun get(userId: UUID, id: UUID): DailyPlanResponse {
        val plan = planRepo.findByIdWithActivities(id)
            .orElseThrow { ResourceNotFoundException("Plan $id not found") }
        if (plan.userId != userId) throw ResourceNotFoundException("Plan $id not found")
        return plan.toResponse()
    }

    fun completeActivity(
        userId: UUID,
        planId: UUID,
        activityId: UUID,
        request: CompleteActivityRequest,
    ): DailyPlanResponse {
        val plan = planRepo.findByIdWithActivities(planId)
            .orElseThrow { ResourceNotFoundException("Plan $planId not found") }
        if (plan.userId != userId) throw ResourceNotFoundException("Plan $planId not found")

        val activity = plan.activities.firstOrNull { it.id == activityId }
            ?: throw ResourceNotFoundException("Activity $activityId not found in plan $planId")

        if (!activity.isCompleted) {
            val wasFullyCompleted = plan.isFullyCompleted
            activity.isCompleted = true
            activity.completedAt = System.currentTimeMillis()
            activity.completionRating = request.rating
            activity.completionNotes = request.notes
            activityRepo.save(activity)

            plan.completedActivities++
            plan.isFullyCompleted = plan.completedActivities >= plan.activities.size
            plan.updatedAt = System.currentTimeMillis()
            planRepo.save(plan)

            if (!wasFullyCompleted && plan.isFullyCompleted) {
                progressServiceClient.recordPlanCompleted(userId)
            }
        }

        return planRepo.findByIdWithActivities(plan.id).get().toResponse()
    }

    /**
     * Marks the first uncompleted activity of [activityType] in today's plan as done.
     * Returns true if an activity was marked, false if no matching pending activity was found.
     * Called internally by activity-service after each activity is logged.
     */
    fun completeByType(userId: UUID, activityType: String): Boolean {
        val todayStart = todayStartEpoch()
        val planOpt = planRepo.findByUserIdAndPlanDate(userId, todayStart)
        if (planOpt.isEmpty) return false

        val plan = planRepo.findByIdWithActivities(planOpt.get().id).get()
        if (plan.userId != userId) return false

        val activity = plan.activities.firstOrNull { it.activityType == activityType && !it.isCompleted }
            ?: return false

        val wasFullyCompleted = plan.isFullyCompleted
        activity.isCompleted = true
        activity.completedAt = System.currentTimeMillis()
        activityRepo.save(activity)

        plan.completedActivities++
        plan.isFullyCompleted = plan.completedActivities >= plan.activities.size
        plan.updatedAt = System.currentTimeMillis()
        planRepo.save(plan)

        if (!wasFullyCompleted && plan.isFullyCompleted) {
            progressServiceClient.recordPlanCompleted(userId)
        }

        return true
    }

    private fun todayStartEpoch(): Long =
        Instant.now().atOffset(ZoneOffset.UTC).toLocalDate()
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

fun DailyPlan.toResponse(): DailyPlanResponse {
    val activityResponses = activities.map { it.toActivityResponse() }
    val total = activities.size
    val completed = completedActivities
    return DailyPlanResponse(
        id = id,
        userId = userId,
        date = planDate,
        greeting = greeting,
        motivationalMessage = motivationalMessage,
        totalDurationMinutes = totalDurationMinutes,
        completedActivities = completed,
        totalActivities = total,
        progressPercentage = if (total > 0) completed.toFloat() / total else 0f,
        isFullyCompleted = isFullyCompleted,
        nextActivity = activityResponses.firstOrNull { !it.isCompleted },
        activities = activityResponses,
        aiGeneratedAt = aiGeneratedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun PlanActivity.toActivityResponse() = PlanActivityResponse(
    id = id,
    type = activityType,
    title = title,
    description = description,
    prompt = prompt,
    durationMinutes = durationMinutes,
    order = sortOrder,
    isCompleted = isCompleted,
    completedAt = completedAt,
    completionRating = completionRating,
    completionNotes = completionNotes,
)
