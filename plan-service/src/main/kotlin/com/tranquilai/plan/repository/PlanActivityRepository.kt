package com.tranquilai.plan.repository

import com.tranquilai.plan.entity.PlanActivity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PlanActivityRepository : JpaRepository<PlanActivity, UUID> {
    fun findByPlanIdOrderBySortOrder(planId: UUID): List<PlanActivity>
}
