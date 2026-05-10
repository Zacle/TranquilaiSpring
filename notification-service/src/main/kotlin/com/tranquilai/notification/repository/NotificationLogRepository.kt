package com.tranquilai.notification.repository

import com.tranquilai.notification.entity.NotificationLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NotificationLogRepository : JpaRepository<NotificationLog, UUID> {
    fun findByUserIdOrderBySentAtDesc(userId: UUID, pageable: Pageable): Page<NotificationLog>
}
