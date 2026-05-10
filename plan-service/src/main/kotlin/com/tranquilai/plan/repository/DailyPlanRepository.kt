package com.tranquilai.plan.repository

import com.tranquilai.plan.entity.DailyPlan
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional
import java.util.UUID

interface DailyPlanRepository : JpaRepository<DailyPlan, UUID> {
    fun findByUserIdOrderByPlanDateDesc(userId: UUID): List<DailyPlan>
    fun findByUserIdAndPlanDate(userId: UUID, planDate: Long): Optional<DailyPlan>

    @Query("SELECT p FROM DailyPlan p LEFT JOIN FETCH p.activities WHERE p.id = :id")
    fun findByIdWithActivities(id: UUID): Optional<DailyPlan>
}
