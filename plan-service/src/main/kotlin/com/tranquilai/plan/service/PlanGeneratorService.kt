package com.tranquilai.plan.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tranquilai.plan.client.PlanContextResponse
import com.tranquilai.plan.dto.ai.AiActivity
import com.tranquilai.plan.dto.ai.AiPlanResponse
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Service
class PlanGeneratorService(private val chatClient: ChatClient) {

    private val logger = LoggerFactory.getLogger(PlanGeneratorService::class.java)
    private val mapper = jacksonObjectMapper()

    fun generate(context: PlanContextResponse?, languageCode: String): AiPlanResponse {
        val prompt = buildPrompt(context, languageCode)
        return try {
            val raw = chatClient.prompt()
                .user(prompt)
                .call()
                .content() ?: throw RuntimeException("Empty AI response")
            parseResponse(raw.trim())
        } catch (ex: Exception) {
            logger.warn("AI plan generation failed, using fallback: ${ex.message}")
            fallbackPlan(context?.firstName ?: "there")
        }
    }

    private fun buildPrompt(ctx: PlanContextResponse?, languageCode: String): String {
        val firstName = ctx?.firstName ?: "there"
        val now = Instant.now().atOffset(ZoneOffset.UTC)
        val dateStr = now.toLocalDate()
            .format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH))
        val timeOfDay = when (now.hour) {
            in 5..11 -> "morning"
            in 12..16 -> "afternoon"
            else -> "evening"
        }
        val languageName = languageNames[languageCode] ?: "English"

        val profileSection = if (ctx != null) buildProfileSection(ctx) else ""

        return """
LANGUAGE REQUIREMENT: Respond exclusively in $languageName.

Generate a personalized daily wellness plan for $firstName.

Date: $dateStr
Time of day: $timeOfDay

$profileSection

Generate a JSON response with this EXACT structure (no markdown, no extra text):
{
  "greeting": "Good $timeOfDay, $firstName!",
  "motivationalMessage": "A brief encouraging phrase (max 10 words)",
  "activities": [
    {
      "type": "MOOD_TRACKING",
      "title": "...",
      "description": "...",
      "prompt": null,
      "durationMinutes": 3
    }
  ]
}

STRICT REQUIREMENTS:
1. First activity MUST be type "MOOD_TRACKING" (how are you feeling today?)
2. Second activity MUST be type "CHAT_WITH_AI" (guided conversation with AI companion)
3. Generate 4–5 activities total (including the 2 required above)
4. Total duration MUST be between 10 and 20 minutes
5. Remaining 2–3 activities chosen from: JOURNALING, BREATHING_EXERCISE, MEDITATION, AFFIRMATION
6. Personalise remaining activities based on user's stress causes and concerns
7. For JOURNALING and AFFIRMATION activities, set "prompt" to a relevant reflective question or affirmation text
8. For MOOD_TRACKING and CHAT_WITH_AI, set "prompt" to null
9. motivationalMessage must be very brief (max 10 words)
10. Return ONLY raw JSON — no markdown, no backticks, no conversational text
        """.trimIndent()
    }

    private fun buildProfileSection(ctx: PlanContextResponse): String {
        val lines = mutableListOf<String>()
        lines += "User Profile:"
        ctx.currentFeelingLevel?.let { lines += "- Current feeling: $it" }
        if (ctx.stressCauses.isNotEmpty()) lines += "- Stress factors: ${ctx.stressCauses.joinToString(", ")}"
        if (ctx.currentConcerns.isNotEmpty()) lines += "- Current concerns: ${ctx.currentConcerns.joinToString(", ")}"
        if (ctx.mentalProcessPreferences.isNotEmpty()) lines += "- Preferences: ${ctx.mentalProcessPreferences.joinToString(", ")}"
        if (ctx.personalGoals.isNotEmpty()) lines += "- Goals: ${ctx.personalGoals.joinToString(", ")}"
        ctx.recommendedApproach?.let { lines += "- Recommended approach: $it" }
        lines += "- Urgency level: ${ctx.urgencyLevel}"
        lines += "- Support intensity: ${ctx.supportIntensity}"
        ctx.communicationStyle?.let { lines += "- Communication style: $it" }
        ctx.baselineAnxietyLevel?.let { lines += "- Baseline anxiety (1-10): $it" }
        ctx.baselineStressLevel?.let { lines += "- Baseline stress (1-10): $it" }
        ctx.baselineWellbeingLevel?.let { lines += "- Baseline wellbeing (1-10): $it" }
        return lines.joinToString("\n")
    }

    private fun parseResponse(raw: String): AiPlanResponse {
        val json = extractJson(raw)
        return mapper.readValue(json, AiPlanResponse::class.java)
    }

    private fun extractJson(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start in 0..<end) text.substring(start, end + 1) else text
    }

    private fun fallbackPlan(firstName: String): AiPlanResponse = AiPlanResponse(
        greeting = "Good day, $firstName!",
        motivationalMessage = "Every small step matters.",
        activities = listOf(
            AiActivity("MOOD_TRACKING", "Check In With Yourself", "Take a moment to notice how you're feeling right now.", null, 3),
            AiActivity("CHAT_WITH_AI", "Talk to Your AI Companion", "Share what's on your mind for a supportive conversation.", null, 5),
            AiActivity("BREATHING_EXERCISE", "Box Breathing", "Inhale 4 counts, hold 4, exhale 4, hold 4. Repeat to calm your nervous system.", null, 4),
            AiActivity("JOURNALING", "Gratitude Journal", "Reflect on three good things that happened recently.", "What are three things you're grateful for today?", 5),
        ),
    )

    private companion object {
        val languageNames = mapOf(
            "en" to "English", "es" to "Spanish", "fr" to "French", "de" to "German",
            "pt" to "Portuguese", "ar" to "Arabic", "zh" to "Chinese", "ja" to "Japanese",
            "ru" to "Russian", "ko" to "Korean", "hi" to "Hindi", "it" to "Italian",
        )
    }
}
