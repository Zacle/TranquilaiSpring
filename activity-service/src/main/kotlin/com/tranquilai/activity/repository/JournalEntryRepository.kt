package com.tranquilai.activity.repository

import com.tranquilai.activity.entity.JournalEntry
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface JournalEntryRepository : JpaRepository<JournalEntry, UUID> {
    fun findByUserIdOrderByCreatedAtDesc(userId: UUID, pageable: Pageable): Page<JournalEntry>
    fun findByUserIdAndIsFavoriteTrueOrderByCreatedAtDesc(userId: UUID): List<JournalEntry>
    fun findByUserIdAndCategoryOrderByCreatedAtDesc(userId: UUID, category: String, pageable: Pageable): Page<JournalEntry>
    fun countByUserIdAndCreatedAtBetween(userId: UUID, from: Long, to: Long): Long
}
