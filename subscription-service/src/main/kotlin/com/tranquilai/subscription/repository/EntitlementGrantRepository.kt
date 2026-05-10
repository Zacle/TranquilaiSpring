package com.tranquilai.subscription.repository

import com.tranquilai.subscription.entity.EntitlementGrant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface EntitlementGrantRepository : JpaRepository<EntitlementGrant, UUID> {
    fun findByUserIdAndFeatureAndGrantedTrue(userId: UUID, feature: String): List<EntitlementGrant>
}
