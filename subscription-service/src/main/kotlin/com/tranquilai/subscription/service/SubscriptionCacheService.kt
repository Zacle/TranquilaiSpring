package com.tranquilai.subscription.service

import org.springframework.data.redis.core.Cursor
import org.springframework.data.redis.core.ScanOptions
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SubscriptionCacheService(
    private val redisTemplate: StringRedisTemplate,
) {
    fun evictUser(userId: UUID) {
        deleteByPattern("entitlements::$userId:*")
        deleteByPattern("usage::$userId:*")
    }

    private fun deleteByPattern(pattern: String) {
        val scanOptions = ScanOptions.scanOptions()
            .match(pattern)
            .count(100)
            .build()
        val keysToDelete = mutableSetOf<String>()

        redisTemplate.executeWithStickyConnection { connection ->
            val serializer = redisTemplate.stringSerializer
            connection.keyCommands().scan(scanOptions).use { cursor: Cursor<ByteArray> ->
                cursor.forEach { rawKey ->
                    keysToDelete.add(serializer.deserialize(rawKey) ?: return@forEach)
                }
            }

            if (keysToDelete.isNotEmpty()) {
                // perform deletion while we still have the connection/context
                redisTemplate.delete(keysToDelete)
            }

            null
        }
    }
}
