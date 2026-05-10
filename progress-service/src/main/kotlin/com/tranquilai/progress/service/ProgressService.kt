package com.tranquilai.progress.service

import com.tranquilai.progress.dto.request.UpdateStatsRequest
import com.tranquilai.progress.dto.response.BadgeResponse
import com.tranquilai.progress.dto.response.GrowthArea
import com.tranquilai.progress.dto.response.GrowthAreasResponse
import com.tranquilai.progress.dto.response.NextBadgeInfo
import com.tranquilai.progress.dto.response.ProgressSummaryResponse
import com.tranquilai.progress.dto.response.UserStatsResponse
import com.tranquilai.progress.entity.Badge
import com.tranquilai.progress.entity.UserStats
import com.tranquilai.progress.repository.BadgeRepository
import com.tranquilai.progress.repository.UserStatsRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class ProgressService(
    private val statsRepo: UserStatsRepository,
    private val badgeRepo: BadgeRepository,
) {
    @Transactional(readOnly = true)
    fun getStats(userId: UUID): UserStatsResponse =
        getOrCreateStats(userId).toResponse()

    @Transactional(readOnly = true)
    fun getSummary(userId: UUID): ProgressSummaryResponse {
        val stats = getOrCreateStats(userId)
        val badges = badgeRepo.findByUserIdOrderByAwardedAtDesc(userId).map { it.toResponse() }
        val nextBadge = nextBadgeInfo(stats.plansCompleted, badges.map { it.type }.toSet())
        return ProgressSummaryResponse(
            stats = stats.toResponse(),
            badges = badges,
            recentBadges = badges.take(3),
            nextBadge = nextBadge,
        )
    }

    fun updateStats(userId: UUID, request: UpdateStatsRequest): UserStatsResponse {
        val stats = getOrCreateStats(userId)
        val now = System.currentTimeMillis()

        stats.totalSessions += request.sessionsIncrement
        stats.totalMinutes += request.minutesIncrement
        stats.moodEntriesCount += request.moodEntriesIncrement
        stats.journalEntriesCount += request.journalEntriesIncrement
        stats.totalChatSessions += request.chatSessionsIncrement
        stats.plansCompleted += request.plansCompletedIncrement

        request.streakDays?.let {
            stats.currentStreakDays = it
            if (it > stats.longestStreakDays) stats.longestStreakDays = it
            if (it == 1) stats.streakStartDate = now  // streak just started
        }

        if (request.markDayActive) {
            stats.totalDaysActive++
            stats.lastActiveDate = now
        }

        request.averageMoodScore?.let { stats.averageMoodScore = it }
        stats.updatedAt = now

        val saved = statsRepo.save(stats)
        checkAndAwardPlanBadges(userId, saved.plansCompleted)
        return saved.toResponse()
    }

    @Transactional(readOnly = true)
    fun getBadges(userId: UUID): List<BadgeResponse> =
        badgeRepo.findByUserIdOrderByAwardedAtDesc(userId).map { it.toResponse() }

    @Transactional(readOnly = true)
    fun getGrowthAreas(userId: UUID): GrowthAreasResponse {
        val stats = getOrCreateStats(userId)

        // Score each wellness area on 0–100 scale using soft caps
        val areas = listOf(
            GrowthArea(
                area = "Mood Tracking",
                score = minOf(100, stats.moodEntriesCount * 5),
                recommendation = "Log your mood daily to identify patterns and triggers.",
                isGrowthArea = stats.moodEntriesCount < 10,
            ),
            GrowthArea(
                area = "Journaling",
                score = minOf(100, stats.journalEntriesCount * 5),
                recommendation = "Write in your journal regularly to process emotions and reflect.",
                isGrowthArea = stats.journalEntriesCount < 10,
            ),
            GrowthArea(
                area = "Consistency",
                score = when {
                    stats.longestStreakDays == 0 -> 0
                    else -> minOf(100, (stats.currentStreakDays.toDouble() / stats.longestStreakDays * 100).toInt())
                },
                recommendation = "Build a daily wellness routine to maintain your streak.",
                isGrowthArea = stats.currentStreakDays < 3,
            ),
            GrowthArea(
                area = "Mindful Conversation",
                score = minOf(100, stats.totalChatSessions * 10),
                recommendation = "Chat with the AI companion to explore your thoughts and feelings.",
                isGrowthArea = stats.totalChatSessions < 5,
            ),
            GrowthArea(
                area = "Wellness Practice",
                score = minOf(100, stats.plansCompleted * 3),
                recommendation = "Complete daily wellness plans to build healthy habits.",
                isGrowthArea = stats.plansCompleted < 7,
            ),
        )

        val growthAreas = areas.filter { it.isGrowthArea }
        val focusArea = growthAreas.minByOrNull { it.score }?.area
        val strongestArea = areas.maxByOrNull { it.score }?.area

        return GrowthAreasResponse(
            growthAreas = areas,
            strongestArea = strongestArea,
            focusArea = focusArea,
        )
    }

    private fun getOrCreateStats(userId: UUID): UserStats =
        statsRepo.findByUserId(userId).orElseGet { statsRepo.save(UserStats(userId = userId)) }

    /**
     * Awards badges based exclusively on total daily plans completed — matching the mobile app's
     * 15-milestone progression system.
     */
    private fun checkAndAwardPlanBadges(userId: UUID, plansCompleted: Int) {
        PLAN_BADGES
            .filter { it.plansRequired <= plansCompleted }
            .filter { !badgeRepo.existsByUserIdAndBadgeType(userId, it.type) }
            .forEach { milestone ->
                badgeRepo.save(
                    Badge(
                        userId = userId,
                        badgeType = milestone.type,
                        badgeName = milestone.name,
                        description = milestone.description,
                    )
                )
            }
    }

    private fun nextBadgeInfo(plansCompleted: Int, earnedTypes: Set<String>): NextBadgeInfo? {
        val next = PLAN_BADGES.firstOrNull { it.plansRequired > plansCompleted && it.type !in earnedTypes }
            ?: return null
        val prev = PLAN_BADGES.lastOrNull { it.plansRequired <= plansCompleted }
        val prevCount = prev?.plansRequired ?: 0
        val rangeSize = next.plansRequired - prevCount
        val progressInRange = plansCompleted - prevCount
        return NextBadgeInfo(
            badgeType = next.type,
            badgeName = next.name,
            description = next.description,
            plansRequired = next.plansRequired,
            plansCompleted = plansCompleted,
            progressPercent = if (rangeSize > 0) progressInRange.toFloat() / rangeSize else 0f,
        )
    }
}

// ── Badge milestone definitions (mirrors mobile BadgeType enum exactly) ──────

private data class BadgeMilestone(
    val type: String,
    val name: String,
    val description: String,
    val plansRequired: Int,
    val colorHex: String,
)

private val PLAN_BADGES = listOf(
    BadgeMilestone("MINDFULNESS_MASTER",        "Mindfulness Master",        "Completed your first daily plan",              1,   "#4CAF50"),
    BadgeMilestone("SERENITY_SEEKER",           "Serenity Seeker",           "Completed 3 daily plans",                      3,   "#66BB6A"),
    BadgeMilestone("RESILIENCE_ROCKSTAR",       "Resilience Rockstar",       "Completed a full week of daily plans",         7,   "#FFC107"),
    BadgeMilestone("ZEN_GURU",                  "Zen Guru",                  "Completed 14 daily plans",                     14,  "#7E57C2"),
    BadgeMilestone("STRESS_BUSTER",             "Stress Buster",             "Completed 3 weeks of daily plans",             21,  "#EF5350"),
    BadgeMilestone("WELLNESS_WARRIOR",          "Wellness Warrior",          "Completed a full month of daily plans",        30,  "#42A5F5"),
    BadgeMilestone("POSITIVITY_PATRON",         "Positivity Patron",         "Completed 45 daily plans",                     45,  "#FFCA28"),
    BadgeMilestone("CALM_CRUSADER",             "Calm Crusader",             "Completed 2 months of daily plans",            60,  "#26C6DA"),
    BadgeMilestone("GROWTH_HACKER",             "Growth Hacker",             "Completed 75 daily plans",                     75,  "#9CCC65"),
    BadgeMilestone("EMPOWERMENT_ENTHUSIAST",    "Empowerment Enthusiast",    "Completed 3 months of daily plans",            90,  "#FF7043"),
    BadgeMilestone("MINDFUL_ACHIEVER",          "Mindful Achiever",          "Completed 100 daily plans",                    100, "#FFD54F"),
    BadgeMilestone("SELF_CARE_SUPERSTAR",       "Self-Care Superstar",       "Completed 4 months of daily plans",            120, "#AB47BC"),
    BadgeMilestone("EMOTION_EXPLORER",          "Emotion Explorer",          "Completed 5 months of daily plans",            150, "#5C6BC0"),
    BadgeMilestone("HAPPINESS_HERO",            "Happiness Hero",            "Completed half a year of daily plans",         180, "#EC407A"),
    BadgeMilestone("PEACEFUL_PIONEER",          "Peaceful Pioneer",          "Completed a full year of daily plans",         365, "#26A69A"),
)

private val PLAN_BADGE_COLORS = PLAN_BADGES.associate { it.type to it.colorHex }

fun UserStats.toResponse() = UserStatsResponse(
    id = id,
    userId = userId,
    currentStreak = currentStreakDays,
    longestStreak = longestStreakDays,
    totalDaysActive = totalDaysActive,
    totalActivitiesCompleted = totalSessions,
    totalMoodEntries = moodEntriesCount,
    totalJournalEntries = journalEntriesCount,
    totalChatSessions = totalChatSessions,
    totalMinutesSpent = totalMinutes,
    totalPlansCompleted = plansCompleted,
    lastActiveDate = lastActiveDate,
    streakStartDate = streakStartDate,
    averageMoodScore = averageMoodScore?.toFloat(),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Badge.toResponse() = BadgeResponse(
    id = id,
    userId = userId,
    type = badgeType,
    badgeName = badgeName,
    description = description,
    colorHex = PLAN_BADGE_COLORS[badgeType] ?: "#4CAF50",
    earnedAt = awardedAt,
)
