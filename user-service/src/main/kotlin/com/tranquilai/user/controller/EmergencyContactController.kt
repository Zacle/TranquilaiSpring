package com.tranquilai.user.controller

import com.tranquilai.user.dto.request.CreateEmergencyContactRequest
import com.tranquilai.user.dto.request.UpdateEmergencyContactRequest
import com.tranquilai.user.dto.response.EmergencyContactResponse
import com.tranquilai.user.security.GatewayUser
import com.tranquilai.user.service.EmergencyContactService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/users/me/emergency-contacts")
class EmergencyContactController(
    private val emergencyContactService: EmergencyContactService,
) {

    /** POST /api/users/me/emergency-contacts — create a new contact */
    @PostMapping
    fun create(
        @AuthenticationPrincipal user: GatewayUser,
        @Valid @RequestBody request: CreateEmergencyContactRequest,
    ): ResponseEntity<EmergencyContactResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(emergencyContactService.create(user.id, request))

    /** GET /api/users/me/emergency-contacts — list all contacts */
    @GetMapping
    fun list(
        @AuthenticationPrincipal user: GatewayUser,
    ): ResponseEntity<List<EmergencyContactResponse>> =
        ResponseEntity.ok(emergencyContactService.list(user.id))

    /** GET /api/users/me/emergency-contacts/primary — get primary contact */
    @GetMapping("/primary")
    fun getPrimary(
        @AuthenticationPrincipal user: GatewayUser,
    ): ResponseEntity<EmergencyContactResponse> {
        val contact = emergencyContactService.getPrimary(user.id)
            ?: return ResponseEntity.noContent().build()
        return ResponseEntity.ok(contact)
    }

    /** GET /api/users/me/emergency-contacts/{id} — get a specific contact */
    @GetMapping("/{id}")
    fun get(
        @AuthenticationPrincipal user: GatewayUser,
        @PathVariable id: UUID,
    ): ResponseEntity<EmergencyContactResponse> =
        ResponseEntity.ok(emergencyContactService.get(user.id, id))

    /** PUT /api/users/me/emergency-contacts/{id} — update a contact */
    @PutMapping("/{id}")
    fun update(
        @AuthenticationPrincipal user: GatewayUser,
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateEmergencyContactRequest,
    ): ResponseEntity<EmergencyContactResponse> =
        ResponseEntity.ok(emergencyContactService.update(user.id, id, request))

    /** PATCH /api/users/me/emergency-contacts/{id}/primary — set as primary */
    @PatchMapping("/{id}/primary")
    fun setPrimary(
        @AuthenticationPrincipal user: GatewayUser,
        @PathVariable id: UUID,
    ): ResponseEntity<EmergencyContactResponse> =
        ResponseEntity.ok(emergencyContactService.setPrimary(user.id, id))

    /** DELETE /api/users/me/emergency-contacts/{id} — delete a contact */
    @DeleteMapping("/{id}")
    fun delete(
        @AuthenticationPrincipal user: GatewayUser,
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        emergencyContactService.delete(user.id, id)
        return ResponseEntity.noContent().build()
    }
}
