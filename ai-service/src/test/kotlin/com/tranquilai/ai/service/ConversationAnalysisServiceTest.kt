package com.tranquilai.ai.service

import com.tranquilai.ai.client.EntitlementResponse
import com.tranquilai.ai.client.SubscriptionServiceClient
import com.tranquilai.ai.client.UserServiceClient
import com.tranquilai.ai.document.ChatMessageDocument
import com.tranquilai.ai.document.ConversationDocument
import com.tranquilai.ai.repository.ChatMessageRepository
import com.tranquilai.ai.repository.ConversationRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.ai.chat.client.ChatClient

class ConversationAnalysisServiceTest {

    private val conversationRepo: ConversationRepository = mock(ConversationRepository::class.java)
    private val messageRepo: ChatMessageRepository = mock(ChatMessageRepository::class.java)
    private val chatClient: ChatClient = mock(ChatClient::class.java, RETURNS_DEEP_STUBS)
    private val userServiceClient: UserServiceClient = mock(UserServiceClient::class.java)
    private val subscriptionClient: SubscriptionServiceClient = mock(SubscriptionServiceClient::class.java)
    private val service = ConversationAnalysisService(
        conversationRepo,
        messageRepo,
        chatClient,
        userServiceClient,
        subscriptionClient,
        AiCallExecutor(),
    )

    @Test
    fun `analyzeAndUpdate returns original conversation when not enough messages`() {
        val conversation = conversation()
        `when`(messageRepo.findByConversationIdOrderByTimestampAsc("conv-1")).thenReturn(listOf(message("USER", "hello")))

        val response = service.analyzeAndUpdate(conversation)

        assertEquals(conversation, response)
        verify(conversationRepo, never()).save(anyConversation())
    }

    @Test
    fun `analyzeAndUpdate saves ai title summary topics and mood`() {
        val conversation = conversation()
        `when`(subscriptionClient.checkEntitlement("user-123", "CHAT_ANALYSIS"))
            .thenReturn(EntitlementResponse(allowed = true, plan = "PREMIUM"))
        `when`(messageRepo.findByConversationIdOrderByTimestampAsc("conv-1")).thenReturn(
            listOf(message("USER", "I feel stressed"), message("ASSISTANT", "That sounds hard")),
        )
        `when`(userServiceClient.getFirstName("user-123")).thenReturn("Maya")
        val requestSpec = chatClient.prompt()
        `when`(requestSpec.user(anyString()).call().content())
            .thenReturn(
                "Stress Support",
                "Maya discussed stress.",
                "stress, coping, support",
                """{"moodAtStart":"anxious","moodAtEnd":"calmer"}""",
            )
        clearInvocations(requestSpec)
        `when`(conversationRepo.save(anyConversation())).thenAnswer { it.getArgument<ConversationDocument>(0) }

        val response = service.analyzeAndUpdate(conversation)

        assertEquals("Stress Support", response.title)
        assertEquals("Maya discussed stress.", response.summary)
        assertEquals(listOf("stress", "coping", "support"), response.keyTopics)
        assertEquals("anxious", response.moodAtStart)
        assertEquals("calmer", response.moodAtEnd)
        val promptCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(requestSpec, times(4)).user(promptCaptor.capture())
        assert(promptCaptor.allValues.any { it.contains("between Maya and their wellness companion") })
    }

    @Test
    fun `analyzeAndUpdate saves title and summary but skips premium fields when entitlement is denied`() {
        val conversation = conversation(title = "Basic Title")
        `when`(subscriptionClient.checkEntitlement("user-123", "CHAT_ANALYSIS"))
            .thenReturn(EntitlementResponse(allowed = false, plan = "FREE"))
        `when`(messageRepo.findByConversationIdOrderByTimestampAsc("conv-1")).thenReturn(
            listOf(message("USER", "I feel stressed"), message("ASSISTANT", "That sounds hard")),
        )
        `when`(userServiceClient.getFirstName("user-123")).thenReturn("Maya")
        val requestSpec = chatClient.prompt()
        `when`(requestSpec.user(anyString()).call().content())
            .thenReturn("Stress Support", "Maya discussed stress.")
        clearInvocations(requestSpec)
        `when`(conversationRepo.save(anyConversation())).thenAnswer { it.getArgument<ConversationDocument>(0) }

        val response = service.analyzeAndUpdate(conversation)

        assertEquals("Stress Support", response.title)
        assertEquals("Maya discussed stress.", response.summary)
        assertEquals(emptyList<String>(), response.keyTopics)
        assertEquals(null, response.moodAtStart)
        assertEquals(null, response.moodAtEnd)
        val promptCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(requestSpec, times(2)).user(promptCaptor.capture())
    }

    private fun conversation(title: String? = null) = ConversationDocument(id = "conv-1", userId = "user-123", title = title)

    private fun message(role: String, content: String) = ChatMessageDocument(
        id = "$role-$content",
        conversationId = "conv-1",
        userId = "user-123",
        role = role,
        content = content,
    )

    @Suppress("UNCHECKED_CAST")
    private fun anyConversation(): ConversationDocument {
        org.mockito.Mockito.any(ConversationDocument::class.java)
        return uninitialized()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> uninitialized(): T = null as T
}
