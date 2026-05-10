package com.tranquilai.notification.service

import com.tranquilai.notification.dto.response.NotificationLogResponse
import com.tranquilai.notification.dto.response.PageResponse
import com.tranquilai.notification.entity.NotificationLog
import com.tranquilai.notification.repository.NotificationLogRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class NotificationHistoryService(private val logRepo: NotificationLogRepository) {

    fun getHistory(userId: UUID, page: Int, size: Int): PageResponse<NotificationLogResponse> {
        val result = logRepo.findByUserIdOrderBySentAtDesc(userId, PageRequest.of(page, size))
        return PageResponse(
            content = result.content.map { it.toResponse() },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            last = result.isLast,
        )
    }
}

fun NotificationLog.toResponse() = NotificationLogResponse(
    id = id,
    userId = userId,
    title = title,
    body = body,
    notificationType = notificationType,
    status = status,
    sentAt = sentAt,
)
