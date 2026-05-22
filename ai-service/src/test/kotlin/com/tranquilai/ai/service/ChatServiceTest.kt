package com.tranquilai.ai.service

import com.tranquilai.ai.client.SubscriptionServiceClient
import com.tranquilai.ai.client.UsageResponse
import com.tranquilai.ai.document.ChatMessageDocument
import com.tranquilai.ai.document.ConversationDocument
import com.tranquilai.ai.dto.request.CreateConversationRequest
import com.tranquilai.ai.dto.request.EndConversationRequest
import com.tranquilai.ai.dto.request.SendMessageRequest
import com.tranquilai.ai.exception.PaymentRequiredException
import com.tranquilai.ai.exception.ResourceNotFoundException
import com.tranquilai.ai.messaging.ChatMessageRequestedEvent
import com.tranquilai.ai.repository.ChatMessageRepository
import com.tranquilai.ai.repository.ConversationRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.Mockito.any
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.data.domain.Pageable
import java.util.Optional

class ChatServiceTest {

    private val conversationRepo: ConversationRepository = mock(ConversationRepository::class.java)
    private val messageRepo: ChatMessageRepository = mock(ChatMessageRepository::class.java)
    private val chatClient: ChatClient = mock(ChatClient::class.java, RETURNS_DEEP_STUBS)
    private val analysisService: ConversationAnalysisService = mock(ConversationAnalysisService::class.java)
    private val activityCompletion: ActivityCompletionService = mock(ActivityCompletionService::class.java)
    private val subscriptionClient: SubscriptionServiceClient = mock(SubscriptionServiceClient::class.java)
    private val rabbitTemplate: RabbitTemplate = mock(RabbitTemplate::class.java)
    private val service = ChatService(
        conversationRepo,
        messageRepo,
        chatClient,
        analysisService,
        activityCompletion,
        subscriptionClient,
        rabbitTemplate,
    )

    @Test
    fun `createConversation saves active conversation`() {
        `when`(conversationRepo.save(anyConversation())).thenAnswer { it.getArgument<ConversationDocument>(0) }

        val response = service.createConversation("user-123", CreateConversationRequest(languageCode = "ar"))

        assertEquals("user-123", response.conversation.userId)
        assertEquals("ar", response.conversation.languageCode)
        assertEquals("ACTIVE", response.conversation.status)
        assertTrue(response.messages.isEmpty())
    }

    @Test
    fun `sendMessage throws payment required and does not save messages when usage denied`() {
        val conversation = conversation()
        `when`(conversationRepo.findById("conv-1")).thenReturn(Optional.of(conversation))
        `when`(subscriptionClient.checkUsage("user-123", "AI_CHAT"))
            .thenReturn(UsageResponse(allowed = false, used = 3, limit = 3, remaining = 0, plan = "FREE"))

        assertThrows(PaymentRequiredException::class.java) {
            service.sendMessage("user-123", "conv-1", SendMessageRequest("hello"))
        }

        verify(messageRepo, never()).save(anyMessage())
        verify(subscriptionClient, never()).incrementUsage("user-123", "AI_CHAT")
    }

    @Test
    fun `sendMessage saves user message queues ai response and updates conversation`() {
        val conversation = conversation(messageCount = 0)
        `when`(conversationRepo.findById("conv-1")).thenReturn(Optional.of(conversation))
        `when`(subscriptionClient.checkUsage("user-123", "AI_CHAT")).thenReturn(UsageResponse(allowed = true, plan = "FREE"))
        `when`(messageRepo.findByConversationIdOrderByTimestampAsc("conv-1")).thenReturn(emptyList())
        `when`(messageRepo.countByConversationId("conv-1")).thenReturn(2)
        `when`(messageRepo.save(anyMessage())).thenAnswer { it.getArgument<ChatMessageDocument>(0) }
        `when`(conversationRepo.save(anyConversation())).thenAnswer { it.getArgument<ConversationDocument>(0) }

        val response = service.sendMessage(
            "user-123",
            "conv-1",
            SendMessageRequest(content = "hello", priorGreeting = "Hi there"),
        )

        assertEquals("hello", response.userMessage.content)
        assertEquals(null, response.aiResponse)
        assertEquals("PENDING_AI_RESPONSE", response.status)
        verify(subscriptionClient).incrementUsage("user-123", "AI_CHAT")
        verify(rabbitTemplate).convertAndSend(eq("tranquilai.ai.events"), eq("ai.chat.message"), anyChatMessageRequestedEvent())
        verify(activityCompletion, never()).onChatStarted("user-123")
        val conversationCaptor = ArgumentCaptor.forClass(ConversationDocument::class.java)
        verify(conversationRepo).save(conversationCaptor.capture())
        assertEquals(2, conversationCaptor.value.messageCount)
    }

    @Test
    fun `generateQueuedAiResponse saves ai message and completes chat activity for first exchange`() {
        val conversation = conversation(messageCount = 2)
        val greeting = ChatMessageDocument(
            id = "greeting",
            conversationId = "conv-1",
            userId = "user-123",
            content = "Hi there",
            role = "ASSISTANT",
            timestamp = 1,
        )
        val user = userMessage().copy(timestamp = 2)
        `when`(conversationRepo.findById("conv-1")).thenReturn(Optional.of(conversation))
        `when`(messageRepo.findByConversationIdOrderByTimestampAsc("conv-1")).thenReturn(listOf(greeting, user))
        `when`(messageRepo.countByConversationId("conv-1")).thenReturn(3)
        `when`(messageRepo.save(anyMessage())).thenAnswer { it.getArgument<ChatMessageDocument>(0) }
        `when`(conversationRepo.save(anyConversation())).thenAnswer { it.getArgument<ConversationDocument>(0) }
        `when`(chatClient.prompt(anyPrompt()).call().content()).thenReturn("AI reply")

        service.generateQueuedAiResponse(
            ChatMessageRequestedEvent(
                userId = "user-123",
                conversationId = "conv-1",
                userMessageId = "msg-1",
                content = "hello",
                languageCode = "en",
            )
        )

        val messageCaptor = ArgumentCaptor.forClass(ChatMessageDocument::class.java)
        verify(messageRepo).save(messageCaptor.capture())
        assertEquals("ASSISTANT", messageCaptor.value.role)
        assertEquals("AI reply", messageCaptor.value.content)
        verify(activityCompletion).onChatStarted("user-123")
    }

    @Test
    fun `getConversation blocks access to another user's conversation`() {
        `when`(conversationRepo.findById("conv-1")).thenReturn(Optional.of(conversation(userId = "other-user")))

        assertThrows(ResourceNotFoundException::class.java) {
            service.getConversation("user-123", "conv-1")
        }
    }

    @Test
    fun `endConversation analyzes when requested and completes conversation`() {
        val conversation = conversation()
        `when`(conversationRepo.findById("conv-1")).thenReturn(Optional.of(conversation))
        `when`(conversationRepo.save(anyConversation())).thenAnswer { it.getArgument<ConversationDocument>(0) }

        val response = service.endConversation("user-123", "conv-1", EndConversationRequest(analyze = true))

        assertEquals("COMPLETED", response.status)
        verify(analysisService).analyzeAsync(anyConversation())
    }

    @Test
    fun `deleteConversation deletes messages then conversation`() {
        val conversation = conversation()
        `when`(conversationRepo.findById("conv-1")).thenReturn(Optional.of(conversation))

        service.deleteConversation("user-123", "conv-1")

        verify(messageRepo).deleteByConversationId("conv-1")
        verify(conversationRepo).delete(conversation)
    }

    @Test
    fun `listConversations maps repository results`() {
        `when`(conversationRepo.findByUserIdOrderByLastMessageAtDesc(eqString("user-123"), anyPageable()))
            .thenReturn(listOf(conversation(id = "conv-1", messageCount = 2), conversation(id = "conv-2", messageCount = 2)))

        val response = service.listConversations("user-123", 0, 20)

        assertEquals(listOf("conv-1", "conv-2"), response.map { it.id })
    }

    @Test
    fun `listConversations hides empty placeholder conversations`() {
        `when`(conversationRepo.findByUserIdOrderByLastMessageAtDesc(eqString("user-123"), anyPageable()))
            .thenReturn(listOf(conversation(id = "empty"), conversation(id = "conv-1", messageCount = 2)))

        val response = service.listConversations("user-123", 0, 20)

        assertEquals(listOf("conv-1"), response.map { it.id })
    }

    private fun conversation(
        id: String = "conv-1",
        userId: String = "user-123",
        messageCount: Int = 0,
    ) = ConversationDocument(id = id, userId = userId, messageCount = messageCount)

    private fun userMessage() = ChatMessageDocument(
        id = "msg-1",
        conversationId = "conv-1",
        userId = "user-123",
        content = "hello",
        role = "USER",
    )

    @Suppress("UNCHECKED_CAST")
    private fun anyConversation(): ConversationDocument {
        any(ConversationDocument::class.java)
        return uninitialized()
    }

    @Suppress("UNCHECKED_CAST")
    private fun anyMessage(): ChatMessageDocument {
        any(ChatMessageDocument::class.java)
        return uninitialized()
    }

    @Suppress("UNCHECKED_CAST")
    private fun anyPrompt(): Prompt {
        any(Prompt::class.java)
        return uninitialized()
    }

    @Suppress("UNCHECKED_CAST")
    private fun anyChatMessageRequestedEvent(): ChatMessageRequestedEvent {
        any(ChatMessageRequestedEvent::class.java)
        return uninitialized()
    }

    @Suppress("UNCHECKED_CAST")
    private fun anyPageable(): Pageable {
        any(Pageable::class.java)
        return uninitialized()
    }

    private fun eqString(value: String): String {
        eq(value)
        return value
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> uninitialized(): T = null as T
}
