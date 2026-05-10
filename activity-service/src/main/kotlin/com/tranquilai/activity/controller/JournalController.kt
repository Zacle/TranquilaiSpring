package com.tranquilai.activity.controller

import com.tranquilai.activity.dto.request.CreateJournalEntryRequest
import com.tranquilai.activity.dto.request.UpdateJournalEntryRequest
import com.tranquilai.activity.dto.response.JournalEntryResponse
import com.tranquilai.activity.dto.response.PageResponse
import com.tranquilai.activity.security.GatewayUser
import com.tranquilai.activity.service.JournalService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/journal")
class JournalController(private val journalService: JournalService) {

    /** POST /api/journal */
    @PostMapping
    fun create(
        @AuthenticationPrincipal user: GatewayUser,
        @Valid @RequestBody request: CreateJournalEntryRequest,
    ): ResponseEntity<JournalEntryResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(journalService.create(user.id, request))

    /** GET /api/journal */
    @GetMapping
    fun list(
        @AuthenticationPrincipal user: GatewayUser,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) category: String?,
    ): ResponseEntity<PageResponse<JournalEntryResponse>> =
        ResponseEntity.ok(journalService.list(user.id, page, size, category))

    /** GET /api/journal/favorites */
    @GetMapping("/favorites")
    fun favorites(
        @AuthenticationPrincipal user: GatewayUser,
    ): ResponseEntity<List<JournalEntryResponse>> =
        ResponseEntity.ok(journalService.favorites(user.id))

    /** GET /api/journal/{id} */
    @GetMapping("/{id}")
    fun get(
        @AuthenticationPrincipal user: GatewayUser,
        @PathVariable id: UUID,
    ): ResponseEntity<JournalEntryResponse> =
        ResponseEntity.ok(journalService.get(user.id, id))

    /** PUT /api/journal/{id} */
    @PutMapping("/{id}")
    fun update(
        @AuthenticationPrincipal user: GatewayUser,
        @PathVariable id: UUID,
        @RequestBody request: UpdateJournalEntryRequest,
    ): ResponseEntity<JournalEntryResponse> =
        ResponseEntity.ok(journalService.update(user.id, id, request))

    /** PATCH /api/journal/{id}/favorite — toggle favorite */
    @PatchMapping("/{id}/favorite")
    fun toggleFavorite(
        @AuthenticationPrincipal user: GatewayUser,
        @PathVariable id: UUID,
    ): ResponseEntity<JournalEntryResponse> =
        ResponseEntity.ok(journalService.toggleFavorite(user.id, id))

    /** DELETE /api/journal/{id} */
    @DeleteMapping("/{id}")
    fun delete(
        @AuthenticationPrincipal user: GatewayUser,
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        journalService.delete(user.id, id)
        return ResponseEntity.noContent().build()
    }
}
