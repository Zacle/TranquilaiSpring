package com.tranquilai.plan.controller

import com.tranquilai.plan.dto.request.CompleteActivityRequest
import com.tranquilai.plan.dto.request.GeneratePlanRequest
import com.tranquilai.plan.dto.response.DailyPlanResponse
import com.tranquilai.plan.security.GatewayUser
import com.tranquilai.plan.service.PlanService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

class PlanControllersTest {

    private val user = GatewayUser(UUID.randomUUID(), "user@example.com", "USER")

    @Test
    fun `plan controller delegates main plan operations`() {
        val service: PlanService = mock(PlanService::class.java)
        val controller = PlanController(service)
        val planId = UUID.randomUUID()
        val activityId = UUID.randomUUID()
        val response = planResponse(planId)
        val generateRequest = GeneratePlanRequest(languageCode = "ar", forceRegenerate = true)
        val completeRequest = CompleteActivityRequest(rating = 4, notes = "done")
        `when`(service.getOrGenerate(user.id, generateRequest)).thenReturn(response)
        `when`(service.list(user.id)).thenReturn(listOf(response))
        `when`(service.get(user.id, planId)).thenReturn(response)
        `when`(service.completeActivity(user.id, planId, activityId, completeRequest)).thenReturn(response)

        assertEquals(response, controller.getOrGenerateToday(user, generateRequest).body)
        assertEquals(listOf(response), controller.list(user).body)
        assertEquals(response, controller.get(user, planId).body)
        assertEquals(response, controller.completeActivity(user, planId, activityId, completeRequest).body)
    }

    @Test
    fun `plan controller supplies defaults for nullable request bodies`() {
        val service: PlanService = mock(PlanService::class.java)
        val controller = PlanController(service)
        val planId = UUID.randomUUID()
        val activityId = UUID.randomUUID()
        val response = planResponse(planId)
        `when`(service.getOrGenerate(user.id, GeneratePlanRequest())).thenReturn(response)
        `when`(service.completeActivity(user.id, planId, activityId, CompleteActivityRequest())).thenReturn(response)

        assertEquals(response, controller.getOrGenerateToday(user, null).body)
        assertEquals(response, controller.completeActivity(user, planId, activityId, null).body)
    }

    @Test
    fun `internal controller returns completion flag from service`() {
        val service: PlanService = mock(PlanService::class.java)
        `when`(service.completeByType(user.id, "MEDITATION")).thenReturn(true)

        val response = InternalPlanController(service).completeByType(user.id, "MEDITATION")

        assertEquals(mapOf("completed" to true), response.body)
        verify(service).completeByType(user.id, "MEDITATION")
    }

    private fun planResponse(id: UUID) = DailyPlanResponse(
        id = id,
        userId = user.id,
        date = 1,
        greeting = "Hello",
        motivationalMessage = "Keep going",
        totalDurationMinutes = 10,
        completedActivities = 0,
        totalActivities = 0,
        progressPercentage = 0f,
        isFullyCompleted = false,
        nextActivity = null,
        activities = emptyList(),
        aiGeneratedAt = 1,
        createdAt = 1,
        updatedAt = 1,
    )
}
