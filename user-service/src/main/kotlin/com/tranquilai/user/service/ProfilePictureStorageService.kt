package com.tranquilai.user.service

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.tranquilai.user.exception.InvalidProfilePictureException
import com.tranquilai.user.exception.ProfilePictureUploadException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID

@Service
class ProfilePictureStorageService(
    @Value("\${app.profile-pictures.firebase-storage-bucket:}") private val bucketName: String,
    @Value("\${app.profile-pictures.firebase-service-account-json:}") private val serviceAccountJson: String,
    @Value("\${app.profile-pictures.max-size-bytes:5242880}") private val maxSizeBytes: Long,
) {
    private val logger = LoggerFactory.getLogger(ProfilePictureStorageService::class.java)

    private val allowedContentTypes =
        setOf(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
        )

    private val storage: Storage by lazy {
        if (bucketName.isBlank()) {
            throw ProfilePictureUploadException("Firebase Storage bucket is not configured")
        }

        val builder = StorageOptions.newBuilder()
        if (serviceAccountJson.isNotBlank()) {
            builder.setCredentials(GoogleCredentials.fromStream(ByteArrayInputStream(decodeServiceAccountJson())))
        }
        builder.build().service
    }

    fun uploadProfilePicture(
        userId: UUID,
        file: MultipartFile,
    ): String {
        validate(file)

        return try {
            val contentType = file.contentType ?: "image/jpeg"
            val extension = extensionFor(contentType)
            val token = UUID.randomUUID().toString()
            val objectName = "profile-pictures/$userId/${System.currentTimeMillis()}-$token.$extension"
            val blobInfo =
                BlobInfo
                    .newBuilder(bucketName, objectName)
                    .setContentType(contentType)
                    .setMetadata(mapOf("firebaseStorageDownloadTokens" to token))
                    .build()

            storage.create(blobInfo, file.bytes)

            val encodedObjectName =
                URLEncoder
                    .encode(objectName, StandardCharsets.UTF_8)
                    .replace("+", "%20")
            "https://firebasestorage.googleapis.com/v0/b/$bucketName/o/$encodedObjectName?alt=media&token=$token"
        } catch (ex: ProfilePictureUploadException) {
            throw ex
        } catch (ex: Exception) {
            throw ProfilePictureUploadException("Failed to upload profile picture", ex)
        }
    }

    fun deleteProfilePictures(userId: UUID) {
        if (bucketName.isBlank()) return
        runCatching {
            storage
                .list(bucketName, Storage.BlobListOption.prefix("profile-pictures/$userId/"))
                .iterateAll()
                .forEach { it.delete() }
        }.onFailure { ex ->
            logger.warn("Failed to delete profile pictures for userId=$userId: ${ex.message}")
        }
    }

    private fun validate(file: MultipartFile) {
        if (file.isEmpty) {
            throw InvalidProfilePictureException("Profile picture is required")
        }
        if (file.size > maxSizeBytes) {
            throw InvalidProfilePictureException("Profile picture must be smaller than ${maxSizeBytes / 1024 / 1024} MB")
        }
        val contentType = file.contentType
        if (contentType == null || contentType !in allowedContentTypes) {
            throw InvalidProfilePictureException("Profile picture must be a JPEG, PNG, WebP, or GIF image")
        }
    }

    private fun extensionFor(contentType: String): String =
        when (contentType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg"
        }

    private fun decodeServiceAccountJson(): ByteArray {
        val trimmed = serviceAccountJson.trim()
        return if (trimmed.startsWith("{")) {
            trimmed.toByteArray(StandardCharsets.UTF_8)
        } else {
            Base64.getDecoder().decode(trimmed)
        }
    }
}
