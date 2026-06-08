package com.tranquilai.subscription.repository

import com.tranquilai.subscription.entity.UsageRecord
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

@Repository
interface UsageRecordRepository : JpaRepository<UsageRecord, UUID> {
    fun findByUserIdAndFeatureAndUsageDate(userId: UUID, feature: String, usageDate: LocalDate): Optional<UsageRecord>
    fun deleteByUserId(userId: UUID)

    @Modifying
    @Query("""
        INSERT INTO usage_records (id, user_id, feature, usage_date, count, daily_limit, created_at)
        VALUES (gen_random_uuid(), :userId, :feature, :usageDate, 1, :dailyLimit, NOW())
        ON CONFLICT (user_id, feature, usage_date) DO UPDATE
        SET count = usage_records.count + 1
        WHERE usage_records.count < COALESCE(:dailyLimit, 2147483647)
    """, nativeQuery = true)
    fun upsertIncrement(userId: UUID, feature: String, usageDate: LocalDate, dailyLimit: Int?)
}
