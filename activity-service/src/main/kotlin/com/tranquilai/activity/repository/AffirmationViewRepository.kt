package com.tranquilai.activity.repository

import com.tranquilai.activity.entity.AffirmationView
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AffirmationViewRepository : JpaRepository<AffirmationView, UUID>
