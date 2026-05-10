package com.tranquilai.notification.service

import com.tranquilai.notification.dto.request.UpsertReminderScheduleRequest
import com.tranquilai.notification.dto.response.ReminderScheduleResponse
import com.tranquilai.notification.entity.ReminderSchedule
import com.tranquilai.notification.repository.ReminderScheduleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class ReminderScheduleService(private val repo: ReminderScheduleRepository) {

    /**
     * Called internally by user-service whenever reminder settings change.
     * Replaces all existing schedules for the user with the provided times.
     * If [request.enabled] is false all times are saved as disabled.
     */
    fun upsert(userId: UUID, request: UpsertReminderScheduleRequest): ReminderScheduleResponse {
        repo.deleteAllByUserId(userId)

        val now = System.currentTimeMillis()
        val schedules = request.reminderTimes.map { time ->
            ReminderSchedule(
                userId = userId,
                reminderTime = time,
                frequency = request.frequency,
                enabled = request.enabled,
                updatedAt = now,
            )
        }

        if (schedules.isNotEmpty()) repo.saveAll(schedules)

        return buildResponse(userId, request.enabled, request.frequency, request.reminderTimes, now)
    }

    @Transactional(readOnly = true)
    fun get(userId: UUID): ReminderScheduleResponse? {
        val schedules = repo.findAllByUserId(userId)
        if (schedules.isEmpty()) return null
        val latest = schedules.maxBy { it.updatedAt }
        return buildResponse(
            userId = userId,
            enabled = latest.enabled,
            frequency = latest.frequency,
            times = schedules.map { it.reminderTime },
            updatedAt = latest.updatedAt,
        )
    }

    private fun buildResponse(
        userId: UUID,
        enabled: Boolean,
        frequency: String,
        times: List<String>,
        updatedAt: Long,
    ) = ReminderScheduleResponse(
        userId = userId,
        enabled = enabled,
        frequency = frequency,
        reminderTimes = times.sorted(),
        updatedAt = updatedAt,
    )
}
