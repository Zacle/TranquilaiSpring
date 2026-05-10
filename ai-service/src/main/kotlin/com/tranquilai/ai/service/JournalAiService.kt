package com.tranquilai.ai.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tranquilai.ai.client.SubscriptionServiceClient
import com.tranquilai.ai.dto.request.SummarizeJournalRequest
import com.tranquilai.ai.dto.response.JournalSummaryResponse
import com.tranquilai.ai.exception.PaymentRequiredException
import com.tranquilai.ai.prompt.AiPrompts
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service

@Service
class JournalAiService(
    private val chatClient: ChatClient,
    private val subscriptionServiceClient: SubscriptionServiceClient,
) {

    private val logger = LoggerFactory.getLogger(JournalAiService::class.java)
    private val mapper = ObjectMapper()

    fun summarize(userId: String, request: SummarizeJournalRequest): JournalSummaryResponse {
        val entitlement = subscriptionServiceClient.checkEntitlement(userId, FEATURE_JOURNAL_SUMMARY)
        if (!entitlement.allowed) {
            throw PaymentRequiredException(
                message = "Premium subscription required for AI journal summaries",
                data = mapOf("feature" to FEATURE_JOURNAL_SUMMARY, "plan" to entitlement.plan),
            )
        }

        return summarizeUnchecked(request)
    }

    fun summarizeInternal(request: SummarizeJournalRequest): JournalSummaryResponse {
        return summarizeUnchecked(request)
    }

    private fun summarizeUnchecked(request: SummarizeJournalRequest): JournalSummaryResponse {
        val prompt = AiPrompts.journalSummaryPrompt(
            promptText = request.promptText,
            content = request.content,
            category = request.category,
            stressCauses = request.stressCauses,
            currentConcerns = request.currentConcerns,
            languageCode = request.languageCode,
        )

        return runCatching {
            val raw = chatClient.prompt().user(prompt).call().content()
                ?: return fallback(request.content)
            parseJournalSummary(raw)
        }.onFailure { logger.warn("Journal summarization failed: ${it.message}") }
         .getOrElse { fallback(request.content) }
    }

    private fun parseJournalSummary(raw: String): JournalSummaryResponse {
        val json = extractJson(raw)
        val node = mapper.readTree(json)
        return JournalSummaryResponse(
            summary = node.get("summary")?.asText() ?: "",
            keyThemes = node.get("keyThemes")?.map { it.asText() } ?: emptyList(),
            emotionalTone = node.get("emotionalTone")?.asText() ?: "neutral",
            suggestedFollowUp = node.get("suggestedFollowUp")?.asText()?.takeIf { it.isNotBlank() && it != "null" },
        )
    }

    private fun fallback(content: String): JournalSummaryResponse {
        val preview = content.take(100).let { if (content.length > 100) "$it…" else it }
        return JournalSummaryResponse(
            summary = "You reflected on your thoughts and feelings: \"$preview\"",
            keyThemes = listOf("self-reflection"),
            emotionalTone = "processing",
            suggestedFollowUp = "What would you like to explore further about what you wrote?",
        )
    }

    private fun extractJson(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start in 0..<end) text.substring(start, end + 1) else text
    }

    private companion object {
        const val FEATURE_JOURNAL_SUMMARY = "JOURNAL_SUMMARY"
    }
}
