package com.tranquilai.auth.controller

import com.tranquilai.auth.service.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/internal/users")
@PreAuthorize("hasRole('INTERNAL')")
class InternalAuthController(
    private val authService: AuthService,
) {
    @PutMapping("/{userId}/deactivate")
    fun deactivateUser(@PathVariable userId: UUID): ResponseEntity<Void> {
        authService.deactivateUser(userId)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{userId}")
    fun deleteUser(@PathVariable userId: UUID): ResponseEntity<Void> {
        authService.deleteUser(userId)
        return ResponseEntity.noContent().build()
    }
}
