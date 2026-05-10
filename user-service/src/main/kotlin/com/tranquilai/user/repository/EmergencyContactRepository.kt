package com.tranquilai.user.repository

import com.tranquilai.user.entity.EmergencyContact
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface EmergencyContactRepository : JpaRepository<EmergencyContact, UUID> {
    fun findByUserIdOrderByCreatedAtDesc(userId: UUID): List<EmergencyContact>

    fun findByUserIdAndIsPrimaryTrue(userId: UUID): EmergencyContact?

    fun findByIdAndUserId(id: UUID, userId: UUID): EmergencyContact?

    @Modifying
    @Query("UPDATE EmergencyContact e SET e.isPrimary = false WHERE e.userId = :userId AND e.isPrimary = true")
    fun clearPrimaryForUser(userId: UUID)

    fun deleteByUserId(userId: UUID)
}
