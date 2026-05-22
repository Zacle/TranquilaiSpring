package com.tranquilai.ai.service

import com.tranquilai.ai.client.SubscriptionServiceClient
import com.tranquilai.ai.document.ChatMessageDocument
import com.tranquilai.ai.document.ConversationDocument
import com.tranquilai.ai.dto.request.CreateConversationRequest
import com.tranquilai.ai.dto.request.EndConversationRequest
import com.tranquilai.ai.dto.request.SendMessageRequest
import com.tranquilai.ai.dto.response.ConversationAnalysisResponse
import com.tranquilai.ai.dto.response.ConversationResponse
import com.tranquilai.ai.dto.response.ConversationWithMessagesResponse
import com.tranquilai.ai.dto.response.MessageResponse
import com.tranquilai.ai.dto.response.SendMessageResponse
import com.tranquilai.ai.exception.PaymentRequiredException
import com.tranquilai.ai.exception.ResourceNotFoundException
import com.tranquilai.ai.messaging.AiMessaging
import com.tranquilai.ai.messaging.ChatMessageRequestedEvent
import com.tranquilai.ai.prompt.AiPrompts
import com.tranquilai.ai.repository.ChatMessageRepository
import com.tranquilai.ai.repository.ConversationRepository
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
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

private const val ANALYSIS_THRESHOLD = 5
private const val FEATURE_AI_CHAT = "AI_CHAT"

@Service
class ChatService(
    private val conversationRepo: ConversationRepository,
    private val messageRepo: ChatMessageRepository,
    private val chatClient: ChatClient,
    private val analysisService: ConversationAnalysisService,
    private val activityCompletion: ActivityCompletionService,
    private val subscriptionServiceClient: SubscriptionServiceClient,
    private val rabbitTemplate: RabbitTemplate,
) {
    private val logger = LoggerFactory.getLogger(ChatService::class.java)

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

    fun sendMessage(userId: String, conversationId: String, request: SendMessageRequest): SendMessageResponse {
        val conversation = getConversationOrThrow(conversationId, userId)
        require(conversation.status == "ACTIVE") { "Conversation is not active" }
        enforceAiChatAccess(userId)

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

        val userMsg = messageRepo.save(
            ChatMessageDocument(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                userId = userId,
                content = request.content,
                role = "USER",
            )
        )

        val newCount = messageRepo.countByConversationId(conversationId).toInt()
        conversationRepo.save(conversation.copy(lastMessageAt = System.currentTimeMillis(), messageCount = newCount))

        rabbitTemplate.convertAndSend(
            AiMessaging.Exchange,
            AiMessaging.ChatMessageRoutingKey,
            ChatMessageRequestedEvent(
                userId = userId,
                conversationId = conversationId,
                userMessageId = userMsg.id,
                content = request.content,
                languageCode = request.languageCode,
            ),
        )

        return SendMessageResponse(
            userMessage = userMsg.toResponse(),
            conversationId = conversationId,
            status = "PENDING_AI_RESPONSE",
        )
    }

    fun generateQueuedAiResponse(event: ChatMessageRequestedEvent) {
        val conversation = getConversationOrThrow(event.conversationId, event.userId)
        if (conversation.status != "ACTIVE") {
            logger.info("Skipping AI response for inactive conversation {}", event.conversationId)
            return
        }

        val history = messageRepo.findByConversationIdOrderByTimestampAsc(event.conversationId)
        val userMessageTimestamp = history.find { it.id == event.userMessageId }?.timestamp
        if (userMessageTimestamp == null) {
            logger.warn("Skipping AI response because user message {} was not found", event.userMessageId)
            return
        }
        if (history.any { it.role == "ASSISTANT" && it.timestamp > userMessageTimestamp }) {
            logger.info("Skipping duplicate AI response for user message {}", event.userMessageId)
            return
        }

        val historyForPrompt = history.filterNot { it.id == event.userMessageId }
        val aiMessages = buildAiMessages(event.content, historyForPrompt, event.languageCode)
        val aiContent = runCatching {
            chatClient.prompt(Prompt(aiMessages)).call().content() ?: "I'm here for you."
        }.onFailure { logger.error("AI call failed: ${it.message}", it) }
            .getOrDefault("I'm here with you. Take your time.")

        messageRepo.save(
            ChatMessageDocument(
                id = UUID.randomUUID().toString(),
                conversationId = event.conversationId,
                userId = event.userId,
                content = aiContent,
                role = "ASSISTANT",
            )
        )

        val updatedMessageCount = messageRepo.countByConversationId(event.conversationId).toInt()
        val updatedConversation = conversationRepo.save(
            conversation.copy(lastMessageAt = System.currentTimeMillis(), messageCount = updatedMessageCount)
        )

        if (updatedMessageCount <= 3 || updatedMessageCount % ANALYSIS_THRESHOLD == 0) {
            analysisService.analyzeAsync(updatedConversation)
        }

        if (updatedMessageCount <= 3) {
            activityCompletion.onChatStarted(event.userId)
        }
    }

    fun streamMessage(userId: String, conversationId: String, request: SendMessageRequest): Flux<String> {
        val conversation = getConversationOrThrow(conversationId, userId)
        require(conversation.status == "ACTIVE") { "Conversation is not active" }
        enforceAiChatAccess(userId)

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
                        messageCount = messageRepo.countByConversationId(conversationId).toInt(),
                    )
                )
                if (isFirstStreamMessage) {
                    activityCompletion.onChatStarted(userId)
                }
            }
            .onErrorReturn("I'm here for you.")
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
        val conversation = getConversationOrThrow(conversationId, userId)
        val completed = conversationRepo.save(
            conversation.copy(status = "COMPLETED", endedAt = System.currentTimeMillis())
        )

        if (request.analyze) {
            analysisService.analyzeAsync(completed)
        }

        return completed.toResponse()
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

    private fun buildAiMessages(
        userContent: String,
        history: List<ChatMessageDocument>,
        languageCode: String,
    ): List<Message> {
        val messages = mutableListOf<Message>()
        messages += SystemMessage(AiPrompts.therapistSystemPrompt(languageCode))

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
    id = id,
    userId = userId,
    title = title,
    summary = summary,
    keyTopics = keyTopics,
    moodAtStart = moodAtStart,
    moodAtEnd = moodAtEnd,
    status = status,
    messageCount = messageCount,
    languageCode = languageCode,
    startedAt = startedAt,
    lastMessageAt = lastMessageAt,
    endedAt = endedAt,
)

fun ChatMessageDocument.toResponse() = MessageResponse(
    id = id,
    conversationId = conversationId,
    content = content,
    role = role,
    timestamp = timestamp,
    sentiment = sentiment,
    detectedEmotions = detectedEmotions,
)
