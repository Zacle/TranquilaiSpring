package com.tranquilai.notification.repository

import com.tranquilai.notification.entity.ReminderSchedule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ReminderScheduleRepository : JpaRepository<ReminderSchedule, UUID> {
    fun findAllByUserId(userId: UUID): List<ReminderSchedule>

    fun deleteAllByUserId(userId: UUID)

    /**
     * Finds all enabled schedules whose reminderTime matches the given HH:mm string.
     * The caller filters further by frequency vs. day-of-week.
     */
    @Query("SELECT r FROM ReminderSchedule r WHERE r.enabled = true AND r.reminderTime = :time")
    fun findEnabledByTime(time: String): List<ReminderSchedule>
}
