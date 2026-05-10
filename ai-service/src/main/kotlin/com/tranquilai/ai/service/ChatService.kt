package com.tranquilai.ai.service

import com.tranquilai.ai.document.ChatMessageDocument
import com.tranquilai.ai.document.ConversationDocument
import com.tranquilai.ai.client.SubscriptionServiceClient
import com.tranquilai.ai.dto.request.CreateConversationRequest
import com.tranquilai.ai.dto.request.EndConversationRequest
import com.tranquilai.ai.dto.request.SendMessageRequest
import com.tranquilai.ai.dto.response.*
import com.tranquilai.ai.exception.PaymentRequiredException
import com.tranquilai.ai.exception.ResourceNotFoundException
import com.tranquilai.ai.prompt.AiPrompts
import com.tranquilai.ai.repository.ChatMessageRepository
import com.tranquilai.ai.repository.ConversationRepository
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import java.util.UUID

private const val ANALYSIS_THRESHOLD = 10
private const val FEATURE_AI_CHAT = "AI_CHAT"

@Service
class ChatService(
    private val conversationRepo: ConversationRepository,
    private val messageRepo: ChatMessageRepository,
    private val chatClient: ChatClient,
    private val analysisService: ConversationAnalysisService,
    private val activityCompletion: ActivityCompletionService,
    private val subscriptionServiceClient: SubscriptionServiceClient,
) {
    private val logger = LoggerFactory.getLogger(ChatService::class.java)

    /**
     * Create a new conversation.
     * No greeting is generated here — the AI greeting is fetched separately via
     * POST /api/insights/greeting (stateless, no DB write).
     * The conversation is created only when the user sends their first message.
     */
    fun createConversation(userId: String, request: CreateConversationRequest): ConversationWithMessagesResponse {
        val conversation = conversationRepo.save(
            ConversationDocument(
                id = UUID.randomUUID().toString(),
                userId = userId,
                languageCode = request.languageCode,
            )
        )
        return ConversationWithMessagesResponse(
            conversation = conversation.toResponse(),
            messages = emptyList(),
        )
    }

    /** Send a user message and get an AI response (blocking) */
    fun sendMessage(userId: String, conversationId: String, request: SendMessageRequest): SendMessageResponse {
        val conversation = getConversationOrThrow(conversationId, userId)
        require(conversation.status == "ACTIVE") { "Conversation is not active" }
        enforceAiChatAccess(userId)

        // On the first message, persist the greeting the user saw before saving the user message
        val history = messageRepo.findByConversationIdOrderByTimestampAsc(conversationId)
        val isFirstMessage = history.isEmpty()
        if (isFirstMessage && !request.priorGreeting.isNullOrBlank()) {
            messageRepo.save(
                ChatMessageDocument(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    userId = userId,
                    content = request.priorGreeting,
                    role = "ASSISTANT",
                    timestamp = System.currentTimeMillis() - 1,
                )
            )
        }

        // Save user message
        val userMsg = messageRepo.save(
            ChatMessageDocument(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                userId = userId,
                content = request.content,
                role = "USER",
            )
        )

        // Load stored history excluding the user message we just saved
        val historyForPrompt = messageRepo.findByConversationIdOrderByTimestampAsc(conversationId)
            .dropLast(1)

        // Build Spring AI prompt from stored history (greeting is now persisted, no manual injection needed)
        val aiMessages = buildAiMessages(request.content, historyForPrompt, request.languageCode)
        val aiContent = runCatching {
            chatClient.prompt(Prompt(aiMessages)).call().content() ?: "I'm here for you 💙"
        }.onFailure { logger.error("AI call failed: ${it.message}", it) }
         .getOrDefault("I'm here with you. Take your time 💙")

        // Save AI response
        val aiMsg = messageRepo.save(
            ChatMessageDocument(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                userId = userId,
                content = aiContent,
                role = "ASSISTANT",
            )
        )

        // Update conversation metadata
        val newCount = conversation.messageCount + 2
        val updatedConversation = conversationRepo.save(
            conversation.copy(lastMessageAt = System.currentTimeMillis(), messageCount = newCount)
        )

        // Trigger async analysis every ANALYSIS_THRESHOLD messages (no client involvement needed)
        if (newCount % ANALYSIS_THRESHOLD == 0) {
            analysisService.analyzeAsync(updatedConversation)
        }

        // On first message: mark CHAT_WITH_AI plan activity complete and update progress
        if (isFirstMessage) {
            activityCompletion.onChatStarted(userId)
        }

        return SendMessageResponse(
            userMessage = userMsg.toResponse(),
            aiResponse = aiMsg.toResponse(),
            conversationId = conversationId,
        )
    }

    /** Streaming version — returns token-by-token Flux for SSE */
    fun streamMessage(userId: String, conversationId: String, request: SendMessageRequest): Flux<String> {
        val conversation = getConversationOrThrow(conversationId, userId)
        require(conversation.status == "ACTIVE") { "Conversation is not active" }
        enforceAiChatAccess(userId)

        // On the first message, persist the greeting the user saw before saving the user message
        val streamHistory = messageRepo.findByConversationIdOrderByTimestampAsc(conversationId)
        val isFirstStreamMessage = streamHistory.isEmpty()
        if (isFirstStreamMessage && !request.priorGreeting.isNullOrBlank()) {
            messageRepo.save(
                ChatMessageDocument(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    userId = userId,
                    content = request.priorGreeting,
                    role = "ASSISTANT",
                    timestamp = System.currentTimeMillis() - 1,
                )
            )
        }

        // Save user message synchronously before streaming
        messageRepo.save(
            ChatMessageDocument(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                userId = userId,
                content = request.content,
                role = "USER",
            )
        )

        val historyForStream = messageRepo.findByConversationIdOrderByTimestampAsc(conversationId).dropLast(1)
        val aiMessages = buildAiMessages(request.content, historyForStream, request.languageCode)

        // Collect full response for saving while streaming to client
        val fullResponseBuilder = StringBuilder()
        return chatClient.prompt(Prompt(aiMessages))
            .stream()
            .content()
            .doOnNext { chunk -> fullResponseBuilder.append(chunk) }
            .doOnComplete {
                val fullContent = fullResponseBuilder.toString()
                messageRepo.save(
                    ChatMessageDocument(
                        id = UUID.randomUUID().toString(),
                        conversationId = conversationId,
                        userId = userId,
                        content = fullContent,
                        role = "ASSISTANT",
                    )
                )
                conversationRepo.save(
                    conversation.copy(
                        lastMessageAt = System.currentTimeMillis(),
                        messageCount = conversation.messageCount + 2,
                    )
                )
                // On first message: mark CHAT_WITH_AI plan activity complete and update progress
                if (isFirstStreamMessage) {
                    activityCompletion.onChatStarted(userId)
                }
            }
            .onErrorReturn("I'm here for you 💙")
    }

    fun listConversations(userId: String, page: Int, size: Int): List<ConversationResponse> =
        conversationRepo.findByUserIdOrderByLastMessageAtDesc(userId, PageRequest.of(page, size))
            .map { it.toResponse() }

    fun getConversation(userId: String, conversationId: String): ConversationWithMessagesResponse {
        val conversation = getConversationOrThrow(conversationId, userId)
        val messages = messageRepo.findByConversationIdOrderByTimestampAsc(conversationId)
        return ConversationWithMessagesResponse(
            conversation = conversation.toResponse(),
            messages = messages.map { it.toResponse() },
        )
    }

    fun endConversation(userId: String, conversationId: String, request: EndConversationRequest): ConversationResponse {
        var conversation = getConversationOrThrow(conversationId, userId)

        if (request.analyze) {
            conversation = analysisService.analyzeAndUpdate(conversation)
        }

        return conversationRepo.save(
            conversation.copy(status = "COMPLETED", endedAt = System.currentTimeMillis())
        ).toResponse()
    }

    fun analyzeConversation(userId: String, conversationId: String): ConversationAnalysisResponse {
        val conversation = getConversationOrThrow(conversationId, userId)
        val updated = analysisService.analyzeAndUpdate(conversation)
        return ConversationAnalysisResponse(
            conversationId = conversationId,
            title = updated.title ?: "Wellness Session",
            summary = updated.summary ?: "",
            keyTopics = updated.keyTopics,
            moodAtStart = updated.moodAtStart,
            moodAtEnd = updated.moodAtEnd,
        )
    }

    fun deleteConversation(userId: String, conversationId: String) {
        val conversation = getConversationOrThrow(conversationId, userId)
        messageRepo.deleteByConversationId(conversationId)
        conversationRepo.delete(conversation)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun buildAiMessages(
        userContent: String,
        history: List<ChatMessageDocument>,
        languageCode: String,
    ): List<Message> {
        val messages = mutableListOf<Message>()
        messages += SystemMessage(AiPrompts.therapistSystemPrompt(languageCode))

        // Map stored history to Spring AI message types
        history.forEach { msg ->
            when (msg.role) {
                "USER" -> messages += UserMessage(msg.content)
                "ASSISTANT" -> messages += AssistantMessage(msg.content)
            }
        }
        messages += UserMessage(userContent)
        return messages
    }

    private fun getConversationOrThrow(conversationId: String, userId: String): ConversationDocument {
        val conv = conversationRepo.findById(conversationId)
            .orElseThrow { ResourceNotFoundException("Conversation $conversationId not found") }
        if (conv.userId != userId) throw ResourceNotFoundException("Conversation $conversationId not found")
        return conv
    }

    private fun enforceAiChatAccess(userId: String) {
        val usage = subscriptionServiceClient.checkUsage(userId, FEATURE_AI_CHAT)
        if (!usage.allowed) {
            throw PaymentRequiredException(
                message = "Daily free AI chat limit reached",
                data = mapOf(
                    "feature" to FEATURE_AI_CHAT,
                    "used" to usage.used,
                    "limit" to usage.limit,
                    "remaining" to usage.remaining,
                    "plan" to usage.plan,
                ),
            )
        }
        subscriptionServiceClient.incrementUsage(userId, FEATURE_AI_CHAT)
    }
}

fun ConversationDocument.toResponse() = ConversationResponse(
    id = id, userId = userId, title = title, summary = summary,
    keyTopics = keyTopics, moodAtStart = moodAtStart, moodAtEnd = moodAtEnd,
    status = status, messageCount = messageCount, languageCode = languageCode,
    startedAt = startedAt, lastMessageAt = lastMessageAt, endedAt = endedAt,
)

fun ChatMessageDocument.toResponse() = MessageResponse(
    id = id, conversationId = conversationId, content = content, role = role,
    timestamp = timestamp, sentiment = sentiment, detectedEmotions = detectedEmotions,
)
