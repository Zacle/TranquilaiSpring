package com.tranquilai.activity.service

import com.tranquilai.activity.dto.response.ActivityBreakdownResponse
import com.tranquilai.activity.dto.response.MoodChartResponse
import com.tranquilai.activity.dto.response.MoodDataPoint
import com.tranquilai.activity.repository.BreathingSessionRepository
import com.tranquilai.activity.repository.JournalEntryRepository
import com.tranquilai.activity.repository.MeditationSessionRepository
import com.tranquilai.activity.repository.MoodEntryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

@Service
@Transactional(readOnly = true)
class AnalyticsService(
    private val moodRepo: MoodEntryRepository,
    private val journalRepo: JournalEntryRepository,
    private val breathingRepo: BreathingSessionRepository,
    private val meditationRepo: MeditationSessionRepository,
) {
    private val zone = ZoneOffset.UTC

    fun getMoodChart(userId: UUID, period: String): MoodChartResponse {
        val now = System.currentTimeMillis()
        val slots = generateSlots(period, now)
        val from = slots.first().second  // earliest slot start

        val entries = moodRepo.findByUserIdAndDateRange(userId, from, now)

        val dataPoints = slots.map { (label, slotStart, slotEnd) ->
            val slotEntries = entries.filter { it.createdAt in slotStart..slotEnd }
            MoodDataPoint(
                label = label,
                averageMoodScore = if (slotEntries.isEmpty()) null
                                   else slotEntries.map { it.moodScore }.average().toFloat(),
                entryCount = slotEntries.size,
            )
        }

        val minRequired = when (period) { "month" -> 7; "year" -> 12; else -> 3 }
        val total = entries.size

        return MoodChartResponse(
            period = period,
            data = dataPoints,
            hasEnoughData = total >= minRequired,
            dataNeededCount = maxOf(0, minRequired - total),
        )
    }

    fun getActivityBreakdown(userId: UUID, period: String): ActivityBreakdownResponse {
        val now = System.currentTimeMillis()
        val from = periodStart(period, now)

        val mood       = moodRepo.countByUserIdAndCreatedAtBetween(userId, from, now)
        val journal    = journalRepo.countByUserIdAndCreatedAtBetween(userId, from, now)
        val breathing  = breathingRepo.countByUserIdAndCreatedAtBetween(userId, from, now)
        val meditation = meditationRepo.countByUserIdAndCreatedAtBetween(userId, from, now)

        return ActivityBreakdownResponse(
            period = period,
            from = from,
            to = now,
            moodEntries = mood,
            journalEntries = journal,
            breathingSessions = breathing,
            meditationSessions = meditation,
            total = mood + journal + breathing + meditation,
        )
    }

    // Returns list of (label, slotStart, slotEnd) ordered oldest → newest
    private fun generateSlots(period: String, now: Long): List<Triple<String, Long, Long>> {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return when (period) {
            "month" -> (29 downTo 0).map { daysBack ->
                val day = today.minusDays(daysBack.toLong())
                val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                Triple(day.dayOfMonth.toString(), start, end)
            }
            "year" -> (11 downTo 0).map { monthsBack ->
                val month = today.minusMonths(monthsBack.toLong()).withDayOfMonth(1)
                val start = month.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = month.plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                val label = month.format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH))
                Triple(label, start, end)
            }
            else -> (6 downTo 0).map { daysBack ->  // week
                val day = today.minusDays(daysBack.toLong())
                val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                val label = day.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH))
                Triple(label, start, end)
            }
        }
    }

    private fun periodStart(period: String, now: Long) = when (period) {
        "month" -> now - 30L * 24 * 3600 * 1000
        "year"  -> now - 365L * 24 * 3600 * 1000
        else    -> now - 7L * 24 * 3600 * 1000
    }
}
