package com.tranquilai.progress.repository

import com.tranquilai.progress.entity.Badge
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BadgeRepository : JpaRepository<Badge, UUID> {
    fun findByUserIdOrderByAwardedAtDesc(userId: UUID): List<Badge>
    fun existsByUserIdAndBadgeType(userId: UUID, badgeType: String): Boolean
}
