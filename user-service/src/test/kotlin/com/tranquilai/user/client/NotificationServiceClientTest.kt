package com.tranquilai.user.client

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate
import java.util.UUID

class NotificationServiceClientTest {

    private val restTemplate: RestTemplate = mock(RestTemplate::class.java)
    private val client = NotificationServiceClient(restTemplate, "http://notification-service:8088", "internal-key")

    @Test
    fun `upsertReminderSchedule calls notification internal endpoint with internal key and body`() {
        val userId = UUID.randomUUID()

        client.upsertReminderSchedule(
            userId = userId,
            enabled = true,
            reminderTimes = listOf("08:00", "20:00"),
            frequency = "WEEKDAYS",
        )

        val entityCaptor = ArgumentCaptor.forClass(HttpEntity::class.java)
        verify(restTemplate).exchange(
            eq("http://notification-service:8088/internal/notifications/reminder-schedules/$userId"),
            eq(HttpMethod.PUT),
            entityCaptor.capture(),
            eq(Any::class.java),
        )

        val entity = entityCaptor.value
        val body = entity.body as Map<*, *>
        assertEquals("internal-key", entity.headers.getFirst("X-Internal-Key"))
        assertEquals(true, body["enabled"])
        assertEquals(listOf("08:00", "20:00"), body["reminderTimes"])
        assertEquals("WEEKDAYS", body["frequency"])
    }
}
