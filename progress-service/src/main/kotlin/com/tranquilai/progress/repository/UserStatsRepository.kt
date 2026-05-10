package com.tranquilai.progress.repository

import com.tranquilai.progress.entity.UserStats
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface UserStatsRepository : JpaRepository<UserStats, UUID> {
    fun findByUserId(userId: UUID): Optional<UserStats>
}
