package com.tranquilai.activity.repository

import com.tranquilai.activity.entity.MoodEntry
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface MoodEntryRepository : JpaRepository<MoodEntry, UUID> {
    fun findByUserIdOrderByCreatedAtDesc(userId: UUID, pageable: Pageable): Page<MoodEntry>

    @Query("SELECT e FROM MoodEntry e WHERE e.userId = :userId AND e.createdAt BETWEEN :from AND :to ORDER BY e.createdAt DESC")
    fun findByUserIdAndDateRange(userId: UUID, from: Long, to: Long): List<MoodEntry>

    fun countByUserIdAndCreatedAtBetween(userId: UUID, from: Long, to: Long): Long

    fun findFirstByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId: UUID, from: Long, to: Long): MoodEntry?
}
