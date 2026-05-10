package com.tranquilai.user.repository

import com.tranquilai.user.entity.MentalHealthProfile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface MentalHealthProfileRepository : JpaRepository<MentalHealthProfile, UUID> {
    fun findByUserId(userId: UUID): Optional<MentalHealthProfile>
    fun existsByUserId(userId: UUID): Boolean
}
