package com.tranquilai.plan.service

import com.tranquilai.plan.client.PlanContextResponse
import com.tranquilai.plan.client.ProgressServiceClient
import com.tranquilai.plan.client.UserServiceClient
import com.tranquilai.plan.dto.ai.AiActivity
import com.tranquilai.plan.dto.ai.AiPlanResponse
import com.tranquilai.plan.dto.request.CompleteActivityRequest
import com.tranquilai.plan.dto.request.GeneratePlanRequest
import com.tranquilai.plan.entity.DailyPlan
import com.tranquilai.plan.entity.PlanActivity
import com.tranquilai.plan.exception.ResourceNotFoundException
import com.tranquilai.plan.repository.DailyPlanRepository
import com.tranquilai.plan.repository.PlanActivityRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.Optional
import java.util.UUID

class PlanServiceTest {

    private val planRepo: DailyPlanRepository = mock(DailyPlanRepository::class.java)
    private val activityRepo: PlanActivityRepository = mock(PlanActivityRepository::class.java)
    private val planGenerator: PlanGeneratorService = mock(PlanGeneratorService::class.java)
    private val userServiceClient: UserServiceClient = mock(UserServiceClient::class.java)
    private val progressServiceClient: ProgressServiceClient = mock(ProgressServiceClient::class.java)
    private val service = PlanService(planRepo, activityRepo, planGenerator, userServiceClient, progressServiceClient)

    @Test
    fun `getOrGenerate returns existing plan for today unless forced`() {
        val userId = UUID.randomUUID()
        val plan = plan(userId = userId).withActivity("MOOD_TRACKING", "Mood", 0)
        `when`(planRepo.findByUserIdAndPlanDate(eqUuid(userId), anyLongValue())).thenReturn(Optional.of(plan))
        `when`(planRepo.findByIdWithActivities(plan.id)).thenReturn(Optional.of(plan))

        val response = service.getOrGenerate(userId, GeneratePlanRequest(forceRegenerate = false))

        assertEquals(plan.id, response.id)
        assertEquals(1, response.totalActivities)
        verify(planGenerator, never()).generate(anyNullableContext(), anyStringValue())
    }

    @Test
    fun `getOrGenerate generates plan from user context and AI activities when missing`() {
        val userId = UUID.randomUUID()
        val context = PlanContextResponse(userId, "Alex", "calm")
        val savedPlanCaptor = ArgumentCaptor.forClass(DailyPlan::class.java)
        val savedActivities = mutableListOf<PlanActivity>()
        `when`(planRepo.findByUserIdAndPlanDate(eqUuid(userId), anyLongValue())).thenReturn(Optional.empty())
        `when`(userServiceClient.getPlanContext(userId)).thenReturn(context)
        `when`(planGenerator.generate(context, "es")).thenReturn(aiPlan())
        `when`(planRepo.save(savedPlanCaptor.capture())).thenAnswer { it.getArgument<DailyPlan>(0) }
        `when`(activityRepo.save(anyActivity())).thenAnswer {
            val activity = it.getArgument<PlanActivity>(0)
            savedActivities += activity
            activity.plan.activities += activity
            activity
        }
        `when`(planRepo.findByIdWithActivities(anyUuid())).thenAnswer { Optional.of(savedPlanCaptor.value) }

        val response = service.getOrGenerate(userId, GeneratePlanRequest(languageCode = "es"))

        assertEquals("Good morning, Alex!", response.greeting)
        assertEquals(8, response.totalDurationMinutes)
        assertEquals(listOf("MOOD_TRACKING", "CHAT_WITH_AI"), response.activities.map { it.type })
        assertEquals(listOf(0, 1), savedActivities.map { it.sortOrder })
        verify(userServiceClient).getPlanContext(userId)
    }

    @Test
    fun `force regenerate ignores today's existing plan`() {
        val userId = UUID.randomUUID()
        val existing = plan(userId = userId)
        val generated = plan(userId = userId, greeting = "Generated").withActivity("MOOD_TRACKING", "Mood", 0)
        `when`(planRepo.findByUserIdAndPlanDate(eqUuid(userId), anyLongValue())).thenReturn(Optional.of(existing))
        `when`(userServiceClient.getPlanContext(userId)).thenReturn(null)
        `when`(planGenerator.generate(null, "en")).thenReturn(aiPlan())
        `when`(planRepo.save(anyPlan())).thenReturn(generated)
        `when`(activityRepo.save(anyActivity())).thenAnswer {
            val activity = it.getArgument<PlanActivity>(0)
            generated.activities += activity
            activity
        }
        `when`(planRepo.findByIdWithActivities(generated.id)).thenReturn(Optional.of(generated))

        val response = service.getOrGenerate(userId, GeneratePlanRequest(forceRegenerate = true))

        assertEquals(generated.id, response.id)
        verify(planGenerator).generate(null, "en")
    }

    @Test
    fun `list and get return only the requesting user's plans`() {
        val userId = UUID.randomUUID()
        val plan = plan(userId = userId).withActivity("MOOD_TRACKING", "Mood", 0)
        `when`(planRepo.findByUserIdOrderByPlanDateDesc(userId)).thenReturn(listOf(plan))
        `when`(planRepo.findByIdWithActivities(plan.id)).thenReturn(Optional.of(plan))

        assertEquals(listOf(plan.id), service.list(userId).map { it.id })
        assertEquals(plan.id, service.get(userId, plan.id).id)

        val otherPlan = plan(userId = UUID.randomUUID())
        `when`(planRepo.findByIdWithActivities(otherPlan.id)).thenReturn(Optional.of(otherPlan))
        assertThrows(ResourceNotFoundException::class.java) { service.get(userId, otherPlan.id) }
    }

    @Test
    fun `completeActivity marks activity complete once and updates plan progress`() {
        val userId = UUID.randomUUID()
        val plan = plan(userId = userId)
            .withActivity("MOOD_TRACKING", "Mood", 0)
            .withActivity("JOURNALING", "Journal", 1)
        val activity = plan.activities.first()
        `when`(planRepo.findByIdWithActivities(plan.id)).thenReturn(Optional.of(plan))
        `when`(activityRepo.save(anyActivity())).thenAnswer { it.getArgument<PlanActivity>(0) }
        `when`(planRepo.save(anyPlan())).thenAnswer { it.getArgument<DailyPlan>(0) }

        val response = service.completeActivity(
            userId,
            plan.id,
            activity.id,
            CompleteActivityRequest(rating = 5, notes = "helpful"),
        )

        assertTrue(activity.isCompleted)
        assertEquals(5, activity.completionRating)
        assertEquals("helpful", activity.completionNotes)
        assertEquals(1, response.completedActivities)
        assertEquals(0.5f, response.progressPercentage)
        verify(progressServiceClient, never()).recordPlanCompleted(userId)

        service.completeActivity(userId, plan.id, activity.id, CompleteActivityRequest(rating = 1))
        assertEquals(1, plan.completedActivities)
    }

    @Test
    fun `completeActivity rejects missing plan wrong owner and missing activity`() {
        val userId = UUID.randomUUID()
        val planId = UUID.randomUUID()
        `when`(planRepo.findByIdWithActivities(planId)).thenReturn(Optional.empty())
        assertThrows(ResourceNotFoundException::class.java) {
            service.completeActivity(userId, planId, UUID.randomUUID(), CompleteActivityRequest())
        }

        val otherPlan = plan(userId = UUID.randomUUID())
        `when`(planRepo.findByIdWithActivities(otherPlan.id)).thenReturn(Optional.of(otherPlan))
        assertThrows(ResourceNotFoundException::class.java) {
            service.completeActivity(userId, otherPlan.id, UUID.randomUUID(), CompleteActivityRequest())
        }

        val ownPlan = plan(userId = userId).withActivity("MOOD_TRACKING", "Mood", 0)
        `when`(planRepo.findByIdWithActivities(ownPlan.id)).thenReturn(Optional.of(ownPlan))
        assertThrows(ResourceNotFoundException::class.java) {
            service.completeActivity(userId, ownPlan.id, UUID.randomUUID(), CompleteActivityRequest())
        }
    }

    @Test
    fun `completeByType completes first matching pending activity in today's plan`() {
        val userId = UUID.randomUUID()
        val plan = plan(userId = userId)
            .withActivity("JOURNALING", "Done Journal", 0, completed = true)
            .withActivity("JOURNALING", "Pending Journal", 1)
            .withActivity("MEDITATION", "Meditate", 2)
        `when`(planRepo.findByUserIdAndPlanDate(eqUuid(userId), anyLongValue())).thenReturn(Optional.of(plan))
        `when`(planRepo.findByIdWithActivities(plan.id)).thenReturn(Optional.of(plan))

        val completed = service.completeByType(userId, "JOURNALING")

        assertTrue(completed)
        assertTrue(plan.activities[1].isCompleted)
        assertEquals(2, plan.completedActivities)
        assertFalse(plan.isFullyCompleted)
        verify(progressServiceClient, never()).recordPlanCompleted(userId)
    }

    @Test
    fun `completeActivity records progress once when plan becomes fully complete`() {
        val userId = UUID.randomUUID()
        val plan = plan(userId = userId)
            .withActivity("MOOD_TRACKING", "Mood", 0, completed = true)
            .withActivity("JOURNALING", "Journal", 1)
        val activity = plan.activities.last()
        `when`(planRepo.findByIdWithActivities(plan.id)).thenReturn(Optional.of(plan))
        `when`(activityRepo.save(anyActivity())).thenAnswer { it.getArgument<PlanActivity>(0) }
        `when`(planRepo.save(anyPlan())).thenAnswer { it.getArgument<DailyPlan>(0) }

        service.completeActivity(userId, plan.id, activity.id, CompleteActivityRequest())
        service.completeActivity(userId, plan.id, activity.id, CompleteActivityRequest())

        assertTrue(plan.isFullyCompleted)
        verify(progressServiceClient).recordPlanCompleted(userId)
    }

    @Test
    fun `completeByType returns false when no current plan or matching pending activity exists`() {
        val userId = UUID.randomUUID()
        `when`(planRepo.findByUserIdAndPlanDate(eqUuid(userId), anyLongValue())).thenReturn(Optional.empty())
        assertFalse(service.completeByType(userId, "MEDITATION"))

        val plan = plan(userId = userId).withActivity("MEDITATION", "Done", 0, completed = true)
        `when`(planRepo.findByUserIdAndPlanDate(eqUuid(userId), anyLongValue())).thenReturn(Optional.of(plan))
        `when`(planRepo.findByIdWithActivities(plan.id)).thenReturn(Optional.of(plan))
        assertFalse(service.completeByType(userId, "MEDITATION"))
    }

    private fun plan(
        id: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        greeting: String = "Hello",
        planDate: Long = 1000,
    ) = DailyPlan(
        id = id,
        userId = userId,
        planDate = planDate,
        greeting = greeting,
        motivationalMessage = "Keep going",
        totalDurationMinutes = 10,
    )

    private fun DailyPlan.withActivity(
        type: String,
        title: String,
        order: Int,
        completed: Boolean = false,
    ): DailyPlan {
        val activity = PlanActivity(
            plan = this,
            activityType = type,
            title = title,
            description = "$title description",
            durationMinutes = 5,
            sortOrder = order,
            isCompleted = completed,
        )
        activities += activity
        if (completed) completedActivities++
        isFullyCompleted = completedActivities >= activities.size
        return this
    }

    private fun aiPlan() = AiPlanResponse(
        greeting = "Good morning, Alex!",
        motivationalMessage = "Breathe into today.",
        activities = listOf(
            AiActivity("MOOD_TRACKING", "Mood Check", "Notice how you feel.", null, 3),
            AiActivity("CHAT_WITH_AI", "Talk It Through", "Share what's present.", null, 5),
        ),
    )

    private fun eqUuid(value: UUID): UUID {
        org.mockito.Mockito.eq(value)
        return value
    }

    private fun anyLongValue(): Long {
        any(Long::class.java)
        return 0L
    }

    private fun anyStringValue(): String {
        any(String::class.java)
        return ""
    }

    private fun anyUuid(): UUID {
        any(UUID::class.java)
        return UUID.randomUUID()
    }

    private fun anyPlan(): DailyPlan {
        any(DailyPlan::class.java)
        return uninitialized()
    }

    private fun anyActivity(): PlanActivity {
        any(PlanActivity::class.java)
        return uninitialized()
    }

    private fun anyNullableContext(): PlanContextResponse? {
        any(PlanContextResponse::class.java)
        return null
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> uninitialized(): T = null as T
}
