package com.tranquilai.ai.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tranquilai.ai.client.SubscriptionServiceClient
import com.tranquilai.ai.client.UserServiceClient
import com.tranquilai.ai.document.ConversationDocument
import com.tranquilai.ai.prompt.AiPrompts
import com.tranquilai.ai.repository.ChatMessageRepository
import com.tranquilai.ai.repository.ConversationRepository
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class ConversationAnalysisService(
    private val conversationRepo: ConversationRepository,
    private val messageRepo: ChatMessageRepository,
    private val chatClient: ChatClient,
    private val userServiceClient: UserServiceClient,
    private val subscriptionServiceClient: SubscriptionServiceClient,
    private val aiCallExecutor: AiCallExecutor,
) {
    private val logger = LoggerFactory.getLogger(ConversationAnalysisService::class.java)
    private val mapper = ObjectMapper()

    @Async("analysisExecutor")
    fun analyzeAsync(conversation: ConversationDocument) {
        runCatching { analyzeAndUpdate(conversation) }
            .onFailure { logger.warn("Async analysis failed for ${conversation.id}: ${it.message}") }
    }

    fun analyzeAndUpdate(conversation: ConversationDocument): ConversationDocument {
        val messages = messageRepo.findByConversationIdOrderByTimestampAsc(conversation.id)
        if (messages.size < 2) return conversation

        val messagesText = messages.joinToString("\n") { "[${it.role}]: ${it.content}" }
        val firstName = userServiceClient.getFirstName(conversation.userId) ?: "the user"

        val title = aiCallExecutor.execute("conversation title", fallback = { null }) {
            chatClient.prompt().user(AiPrompts.conversationTitlePrompt(messagesText, conversation.languageCode)).call().content()?.trim()
        }

        val summary = aiCallExecutor.execute("conversation summary", fallback = { null }) {
            chatClient.prompt().user(AiPrompts.conversationSummaryPrompt(messagesText, firstName, conversation.languageCode)).call().content()?.trim()
        }

        val entitlement = subscriptionServiceClient.checkEntitlement(conversation.userId, FEATURE_CHAT_ANALYSIS)
        val keyTopics =
            if (entitlement.allowed) {
                aiCallExecutor.execute("conversation topics", fallback = { emptyList() }) {
                    chatClient.prompt().user(AiPrompts.conversationTopicsPrompt(messagesText, conversation.languageCode)).call().content()
                        ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                }
            } else {
                logger.info("Skipping premium chat topic analysis for userId=${conversation.userId} plan=${entitlement.plan}")
                emptyList()
            }

        val (moodAtStart, moodAtEnd) =
            if (entitlement.allowed) {
                aiCallExecutor.execute("conversation mood", fallback = { null to null }) {
                    val raw = chatClient.prompt().user(AiPrompts.conversationMoodPrompt(messagesText, conversation.languageCode)).call().content() ?: "{}"
                    val json = raw.let { it.substring(it.indexOf('{'), it.lastIndexOf('}') + 1) }
                    val node = mapper.readTree(json)
                    node.get("moodAtStart")?.asText() to node.get("moodAtEnd")?.asText()
                }
            } else {
                null to null
            }

        return conversationRepo.save(
            conversation.copy(
                title = title ?: conversation.title,
                summary = summary ?: conversation.summary,
                keyTopics = keyTopics.ifEmpty { conversation.keyTopics },
                moodAtStart = moodAtStart ?: conversation.moodAtStart,
                moodAtEnd = moodAtEnd ?: conversation.moodAtEnd,
            )
        )
    }

    private companion object {
        const val FEATURE_CHAT_ANALYSIS = "CHAT_ANALYSIS"
    }
}
