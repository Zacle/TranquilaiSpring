package com.tranquilai.user.repository

import com.tranquilai.user.entity.UserSettings
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface UserSettingsRepository : JpaRepository<UserSettings, UUID> {
    fun findByUserId(userId: UUID): Optional<UserSettings>
    fun deleteByUserId(userId: UUID)
}
