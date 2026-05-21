package com.tranquilai.auth.repository

import com.tranquilai.auth.entity.OutboxEvent
import com.tranquilai.auth.entity.OutboxEventStatus
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OutboxEventRepository : JpaRepository<OutboxEvent, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select e from OutboxEvent e
        where e.status = :status and e.nextAttemptAt <= :now
        order by e.createdAt asc
        """,
    )
    fun findDueEventsForUpdate(
        @Param("status") status: OutboxEventStatus,
        @Param("now") now: Long,
        pageable: Pageable,
    ): List<OutboxEvent>
}
