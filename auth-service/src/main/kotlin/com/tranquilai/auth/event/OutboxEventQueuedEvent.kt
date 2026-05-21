package com.tranquilai.auth.event

import java.util.UUID

data class OutboxEventQueuedEvent(
    val eventId: UUID,
)
