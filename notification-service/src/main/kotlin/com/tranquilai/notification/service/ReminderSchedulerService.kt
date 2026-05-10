package com.tranquilai.notification.service

import com.tranquilai.notification.repository.ReminderScheduleRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service
class ReminderSchedulerService(
    private val scheduleRepo: ReminderScheduleRepository,
    private val fcmService: FcmService,
) {
    private val logger = LoggerFactory.getLogger(ReminderSchedulerService::class.java)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Fires every minute (seconds=0). Checks which users have a reminder scheduled
     * for the current UTC HH:mm, respects their frequency preference, then sends
     * an FCM push to all their active device tokens.
     */
    @Scheduled(cron = "0 * * * * *")
    fun fireReminders() {
        val now = LocalTime.now(ZoneOffset.UTC)
        val currentTime = now.format(timeFormatter)
        val today = java.time.LocalDate.now(ZoneOffset.UTC).dayOfWeek

        val schedules = scheduleRepo.findEnabledByTime(currentTime)
        if (schedules.isEmpty()) return

        logger.debug("Reminder tick $currentTime — ${schedules.size} schedule(s) matched")

        for (schedule in schedules) {
            if (!shouldFireToday(schedule.frequency, today)) continue

            val payload = PushPayload(
                title = "Time for your wellness check-in",
                body = "Your daily plan is ready. Take a moment for yourself today.",
                notificationType = "DAILY_REMINDER",
                data = mapOf("screen" to "home"),
            )

            val result = runCatching { fcmService.sendToUser(schedule.userId, payload) }
                .getOrElse { ex ->
                    logger.error("Failed to send reminder to user ${schedule.userId}: ${ex.message}", ex)
                    PushResult(sent = 0, failed = 1, skipped = 0)
                }

            logger.debug(
                "Reminder sent to user ${schedule.userId}: " +
                        "sent=${result.sent}, failed=${result.failed}, skipped=${result.skipped}"
            )
        }
    }

    private fun shouldFireToday(frequency: String, today: DayOfWeek): Boolean = when (frequency) {
        "WEEKDAYS" -> today !in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        "WEEKENDS" -> today in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        else        -> true  // DAILY
    }
}
