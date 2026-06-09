package com.tranquilai.subscription.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.tranquilai.subscription.dto.request.VerifyPlayPurchaseRequest
import com.tranquilai.subscription.entity.PlanType
import com.tranquilai.subscription.exception.SubscriptionException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriUtils
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.Base64

@Service
class PlayBillingService(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    @param:Value("\${google-play.service-account-json:}") private val serviceAccountJson: String,
    @param:Value("\${google-play.package-name:}") private val packageName: String,
) {
    @Volatile
    private var cachedAccessToken: String? = null

    @Volatile
    private var tokenExpiresAt: Instant = Instant.EPOCH

    fun verifySubscription(request: VerifyPlayPurchaseRequest): VerifiedPlayPurchase {
        val state = getSubscriptionState(
            productId = request.productId,
            purchaseToken = request.purchaseToken,
        )
        if (!state.expiresAt.isAfter(Instant.now())) {
            throw SubscriptionException("Google Play subscription is expired")
        }

        if (!state.paymentCompleted) {
            throw SubscriptionException("Google Play subscription payment is not completed")
        }

        return VerifiedPlayPurchase(
            planType = state.planType,
            startsAt = state.startsAt,
            expiresAt = state.expiresAt,
            autoRenewing = state.autoRenewing,
        )
    }

    fun getSubscriptionState(productId: String, purchaseToken: String): GooglePlaySubscriptionState {
        val node = fetchSubscriptionNode(productId, purchaseToken)
        val expiryMillis = node.get("expiryTimeMillis")?.asText()?.toLongOrNull()
            ?: throw SubscriptionException("Google Play purchase response is missing expiryTimeMillis")
        val paymentState = node.get("paymentState")?.asInt()
        return GooglePlaySubscriptionState(
            planType = planTypeForProduct(productId),
            startsAt = node.get("startTimeMillis")?.asText()?.toLongOrNull()?.let { Instant.ofEpochMilli(it) },
            expiresAt = Instant.ofEpochMilli(expiryMillis),
            autoRenewing = node.get("autoRenewing")?.asBoolean(false) ?: false,
            paymentCompleted = paymentState == null || paymentState in setOf(1, 2),
        )
    }

    fun cancelSubscription(productId: String, purchaseToken: String) {
        if (packageName.isBlank()) {
            throw SubscriptionException("Google Play package name is not configured")
        }
        val credentials = loadCredentials()
        val accessToken = getAccessToken(credentials)

        val subscriptionId = UriUtils.encodePathSegment(productId, StandardCharsets.UTF_8)
        val token = UriUtils.encodePathSegment(purchaseToken, StandardCharsets.UTF_8)
        val appPackage = UriUtils.encodePathSegment(packageName, StandardCharsets.UTF_8)
        val url = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/$appPackage" +
            "/purchases/subscriptions/$subscriptionId/tokens/$token:cancel"

        val headers = HttpHeaders().apply {
            setBearerAuth(accessToken)
            accept = listOf(MediaType.APPLICATION_JSON)
            contentType = MediaType.APPLICATION_JSON
        }

        try {
            restTemplate.exchange(url, HttpMethod.POST, HttpEntity("{}", headers), String::class.java)
        } catch (ex: Exception) {
            throw SubscriptionException("Google Play subscription cancellation failed")
        }
    }

    private fun loadCredentials(): GoogleServiceAccountCredentials {
        if (serviceAccountJson.isBlank()) {
            throw SubscriptionException("Google Play service account is not configured")
        }
        val json = try {
            if (serviceAccountJson.trimStart().startsWith("{")) {
                serviceAccountJson
            } else {
                runCatching { String(Base64.getDecoder().decode(serviceAccountJson)) }
                    .getOrElse { Files.readString(Path.of(serviceAccountJson)) }
            }
        } catch (ex: Exception) {
            throw SubscriptionException("Google Play service account could not be loaded")
        }
        val node = try {
            objectMapper.readTree(json)
        } catch (ex: Exception) {
            throw SubscriptionException("Google Play service account is not valid JSON")
        }
        return GoogleServiceAccountCredentials(
            clientEmail = node.get("client_email")?.asText()
                ?: throw SubscriptionException("Google Play service account is missing client_email"),
            privateKey = node.get("private_key")?.asText()
                ?: throw SubscriptionException("Google Play service account is missing private_key"),
            tokenUri = node.get("token_uri")?.asText() ?: "https://oauth2.googleapis.com/token",
        )
    }

    private fun fetchSubscriptionNode(productId: String, purchaseToken: String): JsonNode =
        try {
            if (packageName.isBlank()) {
                throw SubscriptionException("Google Play package name is not configured")
            }
            val accessToken = getAccessToken(loadCredentials())
            val subscriptionId = UriUtils.encodePathSegment(productId, StandardCharsets.UTF_8)
            val token = UriUtils.encodePathSegment(purchaseToken, StandardCharsets.UTF_8)
            val appPackage = UriUtils.encodePathSegment(packageName, StandardCharsets.UTF_8)
            val url = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/$appPackage" +
                "/purchases/subscriptions/$subscriptionId/tokens/$token"
            val headers = HttpHeaders().apply {
                setBearerAuth(accessToken)
                accept = listOf(MediaType.APPLICATION_JSON)
            }

            restTemplate.exchange(url, HttpMethod.GET, HttpEntity(null, headers), String::class.java)
                .body
                ?.let { objectMapper.readTree(it) }
                ?: throw SubscriptionException("Empty Google Play verification response")
        } catch (ex: SubscriptionException) {
            throw ex
        } catch (ex: Exception) {
            throw SubscriptionException("Google Play purchase verification failed")
        }

    private fun fetchAccessToken(credentials: GoogleServiceAccountCredentials): String {
        val now = Instant.now().epochSecond
        val header = objectMapper.writeValueAsString(mapOf("alg" to "RS256", "typ" to "JWT"))
        val claimSet = objectMapper.writeValueAsString(
            mapOf(
                "iss" to credentials.clientEmail,
                "scope" to "https://www.googleapis.com/auth/androidpublisher",
                "aud" to credentials.tokenUri,
                "iat" to now,
                "exp" to now + 3600,
            ),
        )
        val unsignedJwt = "${base64Url(header.toByteArray())}.${base64Url(claimSet.toByteArray())}"
        val signature = signJwt(unsignedJwt, credentials.privateKey)
        val assertion = "$unsignedJwt.$signature"

        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_FORM_URLENCODED }
        val body = "grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=$assertion"
        val node = restTemplate.postForObject(credentials.tokenUri, HttpEntity(body, headers), String::class.java)
            ?.let { objectMapper.readTree(it) }
            ?: throw SubscriptionException("Empty Google OAuth token response")
        return node.get("access_token")?.asText()
            ?: throw SubscriptionException("Google OAuth token response is missing access_token")
    }

    private fun getAccessToken(credentials: GoogleServiceAccountCredentials): String {
        val token = cachedAccessToken
        if (token != null && Instant.now().isBefore(tokenExpiresAt)) {
            return token
        }

        return fetchAccessToken(credentials).also {
            cachedAccessToken = it
            tokenExpiresAt = Instant.now().plusSeconds(55 * 60)
        }
    }

    private fun signJwt(unsignedJwt: String, privateKeyPem: String): String {
        val privateKeyBytes = privateKeyPem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
            .let { Base64.getDecoder().decode(it) }
        val privateKey = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(privateKeyBytes))
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(privateKey)
        signature.update(unsignedJwt.toByteArray(StandardCharsets.UTF_8))
        return base64Url(signature.sign())
    }

    private fun base64Url(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private fun planTypeForProduct(productId: String): PlanType = when {
        productId.contains("annual", ignoreCase = true) -> PlanType.PREMIUM_ANNUAL
        productId.contains("monthly", ignoreCase = true) -> PlanType.PREMIUM_MONTHLY
        else -> throw SubscriptionException("Unknown Google Play subscription product")
    }
}

data class VerifiedPlayPurchase(
    val planType: PlanType,
    val startsAt: Instant?,
    val expiresAt: Instant,
    val autoRenewing: Boolean,
)

data class GooglePlaySubscriptionState(
    val planType: PlanType,
    val startsAt: Instant?,
    val expiresAt: Instant,
    val autoRenewing: Boolean,
    val paymentCompleted: Boolean,
)

private data class GoogleServiceAccountCredentials(
    val clientEmail: String,
    val privateKey: String,
    val tokenUri: String,
)
