package com.tranquilai.ai.service

import com.tranquilai.ai.client.SubscriptionServiceClient
import com.tranquilai.ai.client.UsageResponse
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
import com.tranquilai.ai.messaging.ChatMessageRequestedEvent
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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID

private const val ANALYSIS_THRESHOLD = 5
private const val FEATURE_AI_CHAT = "AI_CHAT"
private const val MAX_CHAT_HISTORY_MESSAGES = 12
private const val MAX_CHAT_HISTORY_CHARS = 6_000
private const val USAGE_CACHE_TTL_MS = 60_000L

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
    private val usageCache = ConcurrentHashMap<String, CachedUsage>()

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
        // TODO: enforce AI access
        // enforceAiChatAccess(userId)

        val history = messageRepo.findByConversationIdOrderByTimestampAsc(conversationId)
        val isFirstMessage = history.isEmpty()
        val greetingDoc: ChatMessageDocument? = if (isFirstMessage && !request.priorGreeting.isNullOrBlank()) {
            ChatMessageDocument(
                id = request.greetingMessageId ?: UUID.randomUUID().toString(),
                conversationId = conversationId,
                userId = userId,
                content = request.priorGreeting,
                role = "ASSISTANT",
                timestamp = System.currentTimeMillis() - 1,
            ).also { messageRepo.save(it) }
        } else null

        val userMsg = messageRepo.save(
            ChatMessageDocument(
                id = request.messageId ?: UUID.randomUUID().toString(),
                conversationId = conversationId,
                userId = userId,
                content = request.content,
                role = "USER",
            )
        )

        // Build prompt history from what we already have — avoids a redundant DB read
        val historyForPrompt = buildList {
            greetingDoc?.let { add(it) }
            addAll(history)
        }
        val aiMessages = buildAiMessages(request.content, historyForPrompt, request.languageCode)
        val aiContent = runCatching {
            chatClient.prompt(Prompt(aiMessages)).call().content() ?: "I'm here for you."
        }.onFailure { logger.error("AI call failed: ${it.message}", it) }
            .getOrDefault("I'm here with you. Take your time.")

        val aiMsg = messageRepo.save(
            ChatMessageDocument(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                userId = userId,
                content = aiContent,
                role = "ASSISTANT",
            )
        )

        val updatedMessageCount = messageRepo.countByConversationId(conversationId).toInt()
        val updatedConversation = conversationRepo.save(
            conversation.copy(lastMessageAt = System.currentTimeMillis(), messageCount = updatedMessageCount)
        )

        if (updatedMessageCount <= 3 || updatedMessageCount % ANALYSIS_THRESHOLD == 0) {
            analysisService.analyzeAsync(updatedConversation)
        }
        if (isFirstMessage) {
            activityCompletion.onChatStarted(userId)
        }

        return SendMessageResponse(
            userMessage = userMsg.toResponse(),
            aiResponse = aiMsg.toResponse(),
            conversationId = conversationId,
            status = "COMPLETED",
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
        // TODO: do it later after testing
        // enforceAiChatAccess(userId)

        val streamHistory = messageRepo.findByConversationIdOrderByTimestampAsc(conversationId)
        val isFirstStreamMessage = streamHistory.isEmpty()
        if (isFirstStreamMessage && !request.priorGreeting.isNullOrBlank()) {
            messageRepo.save(
                ChatMessageDocument(
                    id = request.greetingMessageId ?: UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    userId = userId,
                    content = request.priorGreeting,
                    role = "ASSISTANT",
                    timestamp = System.currentTimeMillis() - 1,
                )
            )
        }

        val userMessage = messageRepo.save(
            ChatMessageDocument(
                id = request.messageId ?: UUID.randomUUID().toString(),
                conversationId = conversationId,
                userId = userId,
                content = request.content,
                role = "USER",
            )
        )
        conversationRepo.save(
            conversation.copy(
                lastMessageAt = System.currentTimeMillis(),
                messageCount = messageRepo.countByConversationId(conversationId).toInt(),
            )
        )

        val historyForStream = messageRepo.findByConversationIdOrderByTimestampAsc(conversationId)
            .filterNot { it.id == userMessage.id }
        val aiMessages = buildAiMessages(request.content, historyForStream, request.languageCode)

        val fullResponseBuilder = StringBuilder()
        val responsePersisted = AtomicBoolean(false)
        return chatClient.prompt(Prompt(aiMessages))
            .stream()
            .content()
            .onErrorResume { error ->
                logger.error("Streaming AI call failed: ${error.message}", error)
                Flux.just("I'm here with you. Take your time.")
            }
            .doOnNext { chunk -> fullResponseBuilder.append(chunk) }
            .doFinally {
                if (!responsePersisted.compareAndSet(false, true)) return@doFinally
                runCatching {
                    val fullContent = fullResponseBuilder.toString().ifBlank { "I'm here with you. Take your time." }
                    messageRepo.save(
                        ChatMessageDocument(
                            id = request.aiResponseId ?: UUID.randomUUID().toString(),
                            conversationId = conversationId,
                            userId = userId,
                            content = fullContent,
                            role = "ASSISTANT",
                        )
                    )
                    val updatedMessageCount = messageRepo.countByConversationId(conversationId).toInt()
                    val freshConversation = conversationRepo.findById(conversationId)
                        .orElseThrow { ResourceNotFoundException("Conversation $conversationId not found") }
                    val updatedConversation = conversationRepo.save(
                        freshConversation.copy(
                            lastMessageAt = System.currentTimeMillis(),
                            messageCount = updatedMessageCount,
                        )
                    )
                    if (updatedMessageCount <= 3 || updatedMessageCount % ANALYSIS_THRESHOLD == 0) {
                        analysisService.analyzeAsync(updatedConversation)
                    }
                    if (isFirstStreamMessage) {
                        activityCompletion.onChatStarted(userId)
                    }
                }.onFailure { logger.error("Failed to persist streamed AI response: ${it.message}", it) }
            }
    }

    fun listConversations(userId: String, page: Int, size: Int): List<ConversationResponse> =
        conversationRepo.findByUserIdOrderByLastMessageAtDesc(userId, PageRequest.of(page, size))
            .filter { it.messageCount > 0 || !it.summary.isNullOrBlank() }
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

        recentHistoryForPrompt(history).forEach { msg ->
            when (msg.role) {
                "USER" -> messages += UserMessage(msg.content)
                "ASSISTANT" -> messages += AssistantMessage(msg.content)
            }
        }
        messages += UserMessage(userContent)
        return messages
    }

    private fun recentHistoryForPrompt(history: List<ChatMessageDocument>): List<ChatMessageDocument> {
        val selected = ArrayDeque<ChatMessageDocument>()
        var totalChars = 0

        for (message in history.asReversed()) {
            if (message.content.isBlank()) continue
            if (selected.size >= MAX_CHAT_HISTORY_MESSAGES) break

            val nextTotal = totalChars + message.content.length
            if (selected.isNotEmpty() && nextTotal > MAX_CHAT_HISTORY_CHARS) break

            selected.addFirst(message)
            totalChars = nextTotal
        }

        return selected.toList()
    }

    private fun getConversationOrThrow(conversationId: String, userId: String): ConversationDocument {
        val conv = conversationRepo.findById(conversationId)
            .orElseThrow { ResourceNotFoundException("Conversation $conversationId not found") }
        if (conv.userId != userId) throw ResourceNotFoundException("Conversation $conversationId not found")
        return conv
    }

    private fun enforceAiChatAccess(userId: String) {
        val cacheKey = "$userId:$FEATURE_AI_CHAT"
        val usage = cachedUsage(cacheKey) ?: subscriptionServiceClient
            .checkUsage(userId, FEATURE_AI_CHAT)
            .also { usageCache[cacheKey] = CachedUsage(it, System.currentTimeMillis() + USAGE_CACHE_TTL_MS) }

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
        updateCachedUsageAfterIncrement(cacheKey, usage)
    }

    private fun cachedUsage(cacheKey: String): UsageResponse? {
        val cached = usageCache[cacheKey] ?: return null
        return if (cached.expiresAtMillis > System.currentTimeMillis()) {
            cached.response
        } else {
            usageCache.remove(cacheKey)
            null
        }
    }

    private fun updateCachedUsageAfterIncrement(
        cacheKey: String,
        usage: UsageResponse,
    ) {
        usageCache.compute(cacheKey) { _, cached ->
            val current = cached?.response ?: usage
            val remaining = current.remaining?.let { maxOf(0, it - 1) }
            CachedUsage(
                response = current.copy(
                    used = current.used + 1,
                    remaining = remaining,
                    allowed = remaining?.let { it > 0 } ?: current.allowed,
                ),
                expiresAtMillis = System.currentTimeMillis() + USAGE_CACHE_TTL_MS,
            )
        }
    }
}

private data class CachedUsage(
    val response: UsageResponse,
    val expiresAtMillis: Long,
)

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
