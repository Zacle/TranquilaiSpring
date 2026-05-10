package com.tranquilai.content.service

import com.tranquilai.content.entity.ContentFavorite
import com.tranquilai.content.entity.ContentFavoriteId
import com.tranquilai.content.repository.ContentFavoriteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class FavoriteService(private val repo: ContentFavoriteRepository) {

    @Transactional(readOnly = true)
    fun getFavorites(userId: UUID, contentType: String): List<String> =
        repo.findByIdUserIdAndIdContentTypeOrderByCreatedAtDesc(userId, contentType)
            .map { it.id.contentId }

    @Transactional(readOnly = true)
    fun isFavorite(userId: UUID, contentType: String, contentId: String): Boolean =
        repo.existsByIdUserIdAndIdContentTypeAndIdContentId(userId, contentType, contentId)

    fun toggle(userId: UUID, contentType: String, contentId: String): Map<String, Boolean> {
        val isFavorite = repo.existsByIdUserIdAndIdContentTypeAndIdContentId(userId, contentType, contentId)
        return if (isFavorite) {
            repo.deleteByUserIdAndContentTypeAndContentId(userId, contentType, contentId)
            mapOf("isFavorite" to false)
        } else {
            repo.save(ContentFavorite(id = ContentFavoriteId(userId, contentType, contentId)))
            mapOf("isFavorite" to true)
        }
    }
}
