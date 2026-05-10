package com.tranquilai.ai.controller

import com.tranquilai.ai.dto.request.CreateConversationRequest
import com.tranquilai.ai.dto.request.EndConversationRequest
import com.tranquilai.ai.dto.request.SendMessageRequest
import com.tranquilai.ai.service.ChatService
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/api/conversations")
class ChatController(private val chatService: ChatService) {

    @PostMapping
    fun createConversation(
        @RequestHeader("X-User-Id") userId: String,
        @Valid @RequestBody request: CreateConversationRequest,
    ) = ResponseEntity.ok(chatService.createConversation(userId, request))

    @PostMapping("/{conversationId}/messages")
    fun sendMessage(
        @RequestHeader("X-User-Id") userId: String,
        @PathVariable conversationId: String,
        @Valid @RequestBody request: SendMessageRequest,
    ) = ResponseEntity.ok(chatService.sendMessage(userId, conversationId, request))

    @PostMapping("/{conversationId}/messages/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamMessage(
        @RequestHeader("X-User-Id") userId: String,
        @PathVariable conversationId: String,
        @Valid @RequestBody request: SendMessageRequest,
    ): Flux<String> = chatService.streamMessage(userId, conversationId, request)

    @GetMapping
    fun listConversations(
        @RequestHeader("X-User-Id") userId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ) = ResponseEntity.ok(chatService.listConversations(userId, page, size))

    @GetMapping("/{conversationId}")
    fun getConversation(
        @RequestHeader("X-User-Id") userId: String,
        @PathVariable conversationId: String,
    ) = ResponseEntity.ok(chatService.getConversation(userId, conversationId))

    @PutMapping("/{conversationId}/end")
    fun endConversation(
        @RequestHeader("X-User-Id") userId: String,
        @PathVariable conversationId: String,
        @RequestBody request: EndConversationRequest,
    ) = ResponseEntity.ok(chatService.endConversation(userId, conversationId, request))

    @PostMapping("/{conversationId}/analyze")
    fun analyzeConversation(
        @RequestHeader("X-User-Id") userId: String,
        @PathVariable conversationId: String,
    ) = ResponseEntity.ok(chatService.analyzeConversation(userId, conversationId))

    @DeleteMapping("/{conversationId}")
    fun deleteConversation(
        @RequestHeader("X-User-Id") userId: String,
        @PathVariable conversationId: String,
    ): ResponseEntity<Void> {
        chatService.deleteConversation(userId, conversationId)
        return ResponseEntity.noContent().build()
    }
}
