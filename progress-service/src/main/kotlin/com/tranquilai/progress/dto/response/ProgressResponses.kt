package com.tranquilai.progress.dto.response

import java.util.UUID

data class UserStatsResponse(
    val id: UUID,
    val userId: UUID,
    val currentStreak: Int,
    val longestStreak: Int,
    val totalDaysActive: Int,
    val totalActivitiesCompleted: Int,
    val totalMoodEntries: Int,
    val totalJournalEntries: Int,
    val totalChatSessions: Int,
    val totalMinutesSpent: Int,
    val totalPlansCompleted: Int,
    val lastActiveDate: Long?,
    val streakStartDate: Long?,
    val averageMoodScore: Float?,
    val createdAt: Long,
    val updatedAt: Long,
)

data class BadgeResponse(
    val id: UUID,
    val userId: UUID,
    val type: String,
    val badgeName: String,
    val description: String?,
    val colorHex: String,
    val earnedAt: Long,
)

data class ProgressSummaryResponse(
    val stats: UserStatsResponse,
    val badges: List<BadgeResponse>,
    val recentBadges: List<BadgeResponse>,
    val nextBadge: NextBadgeInfo?,
)

data class NextBadgeInfo(
    val badgeType: String,
    val badgeName: String,
    val description: String,
    val plansRequired: Int,
    val plansCompleted: Int,
    val progressPercent: Float,
)

data class GrowthArea(
    val area: String,
    val score: Int,             // 0–100 — higher means more established
    val recommendation: String,
    val isGrowthArea: Boolean,  // true when the area needs attention
)

data class GrowthAreasResponse(
    val growthAreas: List<GrowthArea>,
    val strongestArea: String?,
    val focusArea: String?,     // the single area that needs the most attention
)
