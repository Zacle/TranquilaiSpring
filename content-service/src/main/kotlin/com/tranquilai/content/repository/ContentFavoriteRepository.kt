package com.tranquilai.content.repository

import com.tranquilai.content.entity.ContentFavorite
import com.tranquilai.content.entity.ContentFavoriteId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ContentFavoriteRepository : JpaRepository<ContentFavorite, ContentFavoriteId> {

    fun findByIdUserIdAndIdContentTypeOrderByCreatedAtDesc(
        userId: UUID,
        contentType: String,
    ): List<ContentFavorite>

    fun existsByIdUserIdAndIdContentTypeAndIdContentId(
        userId: UUID,
        contentType: String,
        contentId: String,
    ): Boolean

    @Modifying
    @Query("DELETE FROM ContentFavorite f WHERE f.id.userId = :userId AND f.id.contentType = :contentType AND f.id.contentId = :contentId")
    fun deleteByUserIdAndContentTypeAndContentId(userId: UUID, contentType: String, contentId: String)
}
