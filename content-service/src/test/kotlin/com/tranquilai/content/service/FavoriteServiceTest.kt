package com.tranquilai.content.service

import com.tranquilai.content.entity.ContentFavorite
import com.tranquilai.content.entity.ContentFavoriteId
import com.tranquilai.content.repository.ContentFavoriteRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

class FavoriteServiceTest {

    private val repo: ContentFavoriteRepository = mock(ContentFavoriteRepository::class.java)
    private val service = FavoriteService(repo)

    @Test
    fun `getFavorites returns favorite content ids in repository order`() {
        val userId = UUID.randomUUID()
        `when`(repo.findByIdUserIdAndIdContentTypeOrderByCreatedAtDesc(userId, "meditation")).thenReturn(
            listOf(
                favorite(userId, "meditation", "newer"),
                favorite(userId, "meditation", "older"),
            ),
        )

        val favorites = service.getFavorites(userId, "meditation")

        assertEquals(listOf("newer", "older"), favorites)
    }

    @Test
    fun `isFavorite delegates to repository existence check`() {
        val userId = UUID.randomUUID()
        `when`(repo.existsByIdUserIdAndIdContentTypeAndIdContentId(userId, "breathing", "box_breathing"))
            .thenReturn(true)

        assertTrue(service.isFavorite(userId, "breathing", "box_breathing"))
    }

    @Test
    fun `toggle removes existing favorite`() {
        val userId = UUID.randomUUID()
        `when`(repo.existsByIdUserIdAndIdContentTypeAndIdContentId(userId, "affirmation", "self_worth"))
            .thenReturn(true)

        val result = service.toggle(userId, "affirmation", "self_worth")

        assertFalse(result.getValue("isFavorite"))
        verify(repo).deleteByUserIdAndContentTypeAndContentId(userId, "affirmation", "self_worth")
        verify(repo, never()).save(anyFavorite())
    }

    @Test
    fun `toggle saves missing favorite`() {
        val userId = UUID.randomUUID()
        `when`(repo.existsByIdUserIdAndIdContentTypeAndIdContentId(userId, "journal_prompt", "gratitude"))
            .thenReturn(false)
        `when`(repo.save(anyFavorite())).thenAnswer { it.getArgument<ContentFavorite>(0) }

        val result = service.toggle(userId, "journal_prompt", "gratitude")

        assertTrue(result.getValue("isFavorite"))
        verify(repo).save(anyFavorite())
        verify(repo, never()).deleteByUserIdAndContentTypeAndContentId(userId, "journal_prompt", "gratitude")
    }

    private fun favorite(userId: UUID, contentType: String, contentId: String) =
        ContentFavorite(ContentFavoriteId(userId, contentType, contentId))

    private fun anyFavorite(): ContentFavorite {
        any(ContentFavorite::class.java)
        return uninitialized()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> uninitialized(): T = null as T
}
