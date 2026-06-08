package com.tranquilai.ai.controller

import com.tranquilai.ai.service.ChatService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal/users")
@PreAuthorize("hasRole('INTERNAL')")
class InternalAccountDeletionController(
    private val chatService: ChatService,
) {
    @DeleteMapping("/{userId}/chat-history")
    fun deleteChatHistory(@PathVariable userId: String): ResponseEntity<Void> {
        chatService.deleteUserChatHistory(userId)
        return ResponseEntity.noContent().build()
    }
}
