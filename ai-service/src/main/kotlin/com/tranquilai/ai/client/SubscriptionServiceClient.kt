package com.tranquilai.ai.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.util.concurrent.ConcurrentHashMap

@Component
class SubscriptionServiceClient(
    private val restTemplate: RestTemplate,
    @param:Value("\${app.subscription-service-url}") private val subscriptionServiceUrl: String,
    @param:Value("\${app.internal-service-key}") private val internalKey: String,
) {
    private val logger = LoggerFactory.getLogger(SubscriptionServiceClient::class.java)

    companion object {
        private const val ENTITLEMENT_CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutes
    }

    // Local cache avoids a network hop on every AI/journal request within the same session.
    // Entries are only cached when the user is allowed (denied results are not cached so
    // users don't have to wait a full TTL after upgrading).
    private val entitlementCache = ConcurrentHashMap<String, CachedEntitlement>()

    fun checkUsage(userId: String, feature: String): UsageResponse {
        return try {
            restTemplate.exchange(
                "$subscriptionServiceUrl/internal/subscriptions/usage?userId=$userId&feature=$feature",
                HttpMethod.GET,
                HttpEntity(null, internalHeaders()),
                UsageResponse::class.java,
            ).body ?: UsageResponse(allowed = true, plan = "FREE")
        } catch (ex: Exception) {
            logger.warn("Subscription usage check failed for user $userId feature=$feature: ${ex.message}")
            UsageResponse(allowed = true, plan = "UNKNOWN")
        }
    }

    fun checkEntitlement(userId: String, feature: String): EntitlementResponse {
        val key = "$userId:$feature"
        val cached = entitlementCache[key]
        if (cached != null && cached.expiresAtMillis > System.currentTimeMillis()) {
            return cached.response
        }

        val result = try {
            restTemplate.exchange(
                "$subscriptionServiceUrl/internal/subscriptions/entitlement?userId=$userId&feature=$feature",
                HttpMethod.GET,
                HttpEntity(null, internalHeaders()),
                EntitlementResponse::class.java,
            ).body ?: EntitlementResponse(allowed = true, plan = "FREE")
        } catch (ex: Exception) {
            logger.warn("Subscription entitlement check failed for user $userId feature=$feature: ${ex.message}")
            EntitlementResponse(allowed = true, plan = "UNKNOWN")
        }

        if (result.allowed) {
            entitlementCache[key] = CachedEntitlement(result, System.currentTimeMillis() + ENTITLEMENT_CACHE_TTL_MS)
        } else {
            entitlementCache.remove(key)
        }
        return result
    }

    fun invalidateEntitlementCache(userId: String) {
        entitlementCache.keys.removeIf { it.startsWith("$userId:") }
    }

    fun incrementUsage(userId: String, feature: String) {
        try {
            restTemplate.exchange(
                "$subscriptionServiceUrl/internal/subscriptions/usage/increment?userId=$userId",
                HttpMethod.POST,
                HttpEntity(IncrementUsageRequest(feature), internalHeaders()),
                Void::class.java,
            )
        } catch (ex: Exception) {
            logger.warn("Subscription usage increment failed for user $userId feature=$feature: ${ex.message}")
        }
    }

    private fun internalHeaders() = HttpHeaders().apply {
        set("X-Internal-Key", internalKey)
        contentType = MediaType.APPLICATION_JSON
    }
}

data class UsageResponse(
    val allowed: Boolean = true,
    val used: Int = 0,
    val limit: Int? = null,
    val remaining: Int? = null,
    val plan: String,
)

data class EntitlementResponse(
    val allowed: Boolean = true,
    val remaining: Int? = null,
    val plan: String,
)

data class IncrementUsageRequest(val feature: String)

private data class CachedEntitlement(
    val response: EntitlementResponse,
    val expiresAtMillis: Long,
)
