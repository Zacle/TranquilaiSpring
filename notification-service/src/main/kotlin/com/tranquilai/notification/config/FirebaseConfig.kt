package com.tranquilai.notification.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.ByteArrayInputStream

@Configuration
class FirebaseConfig(
    @param:Value("\${app.firebase-service-account-json:}") private val serviceAccountJson: String,
) {
    private val logger = LoggerFactory.getLogger(FirebaseConfig::class.java)

    @Bean
    fun firebaseApp(): FirebaseApp {
        if (FirebaseApp.getApps().isNotEmpty()) return FirebaseApp.getInstance()

        val credentials = if (serviceAccountJson.isNotBlank()) {
            logger.info("Initializing Firebase from FIREBASE_SERVICE_ACCOUNT_JSON env var")
            GoogleCredentials.fromStream(ByteArrayInputStream(serviceAccountJson.toByteArray()))
        } else {
            logger.info("Initializing Firebase from GOOGLE_APPLICATION_CREDENTIALS / ADC")
            GoogleCredentials.getApplicationDefault()
        }

        val options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .build()

        return FirebaseApp.initializeApp(options)
    }

    @Bean
    fun firebaseMessaging(firebaseApp: FirebaseApp): FirebaseMessaging =
        FirebaseMessaging.getInstance(firebaseApp)
}
