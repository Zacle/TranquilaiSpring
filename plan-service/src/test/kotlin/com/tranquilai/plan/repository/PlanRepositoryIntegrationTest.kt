package com.tranquilai.plan.repository

import com.tranquilai.plan.entity.DailyPlan
import com.tranquilai.plan.entity.PlanActivity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.TestPropertySource
import java.util.UUID

@DataJpaTest
@TestPropertySource(
    properties = [
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false",
    ],
)
class PlanRepositoryIntegrationTest @Autowired constructor(
    private val dailyPlanRepository: DailyPlanRepository,
    private val planActivityRepository: PlanActivityRepository,
    private val entityManager: TestEntityManager,
) {

    @Test
    fun `daily plan repository finds user plans by date and orders newest first`() {
        val userId = UUID.randomUUID()
        val oldPlan = dailyPlanRepository.save(plan(userId, 1000, "Old"))
        val newPlan = dailyPlanRepository.save(plan(userId, 2000, "New"))
        dailyPlanRepository.save(plan(UUID.randomUUID(), 3000, "Other"))

        assertEquals(newPlan.id, dailyPlanRepository.findByUserIdAndPlanDate(userId, 2000).get().id)
        assertEquals(listOf(newPlan.id, oldPlan.id), dailyPlanRepository.findByUserIdOrderByPlanDateDesc(userId).map { it.id })
    }

    @Test
    fun `findByIdWithActivities fetches activities ordered by sort order`() {
        val userId = UUID.randomUUID()
        val plan = dailyPlanRepository.save(plan(userId, 1000, "Plan"))
        planActivityRepository.save(activity(plan, "MEDITATION", "Second", 2))
        planActivityRepository.save(activity(plan, "MOOD_TRACKING", "First", 1))
        planActivityRepository.flush()
        entityManager.clear()

        val found = dailyPlanRepository.findByIdWithActivities(plan.id).get()

        assertEquals(listOf("First", "Second"), found.activities.map { it.title })
        assertEquals(listOf("First", "Second"), planActivityRepository.findByPlanIdOrderBySortOrder(plan.id).map { it.title })
    }

    @Test
    fun `deleting daily plan cascades to activities`() {
        val plan = plan(UUID.randomUUID(), 1000, "Plan")
        val activity = activity(plan, "MOOD_TRACKING", "Mood", 0)
        plan.activities += activity
        val savedPlan = dailyPlanRepository.saveAndFlush(plan)
        val activityId = savedPlan.activities.single().id
        entityManager.clear()

        dailyPlanRepository.deleteById(savedPlan.id)
        dailyPlanRepository.flush()
        entityManager.clear()

        assertTrue(planActivityRepository.findById(activityId).isEmpty)
    }

    private fun plan(userId: UUID, date: Long, greeting: String) = DailyPlan(
        userId = userId,
        planDate = date,
        greeting = greeting,
        motivationalMessage = "Keep going",
        totalDurationMinutes = 5,
    )

    private fun activity(plan: DailyPlan, type: String, title: String, order: Int) = PlanActivity(
        plan = plan,
        activityType = type,
        title = title,
        description = "$title description",
        prompt = "prompt",
        durationMinutes = 5,
        sortOrder = order,
    )
}
