package com.tranquilai.activity.repository

import com.tranquilai.activity.entity.MeditationSession
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MeditationSessionRepository : JpaRepository<MeditationSession, UUID> {
    fun findByUserIdOrderByCreatedAtDesc(userId: UUID, pageable: Pageable): Page<MeditationSession>
    fun countByUserId(userId: UUID): Long
    fun countByUserIdAndCreatedAtBetween(userId: UUID, from: Long, to: Long): Long
    fun findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId: UUID, from: Long, to: Long): List<MeditationSession>
}
