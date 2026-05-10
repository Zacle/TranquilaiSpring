package com.tranquilai.content.controller

import com.tranquilai.content.entity.ContentType
import com.tranquilai.content.security.GatewayUser
import com.tranquilai.content.service.FavoriteService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/breathing")
class BreathingExerciseController(private val favoriteService: FavoriteService) {

    @GetMapping("/favorites")
    fun favorites(
        @AuthenticationPrincipal user: GatewayUser,
    ): ResponseEntity<List<String>> =
        ResponseEntity.ok(favoriteService.getFavorites(user.id, ContentType.BREATHING))

    @PostMapping("/{id}/favorite")
    fun toggleFavorite(
        @AuthenticationPrincipal user: GatewayUser,
        @PathVariable id: String,
    ): ResponseEntity<Map<String, Boolean>> =
        ResponseEntity.ok(favoriteService.toggle(user.id, ContentType.BREATHING, id))
}
