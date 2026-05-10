package com.tranquilai.ai.service

import com.tranquilai.ai.dto.request.GenerateAffirmationRequest
import com.tranquilai.ai.dto.request.GenerateGreetingRequest
import com.tranquilai.ai.dto.request.MoodInsightRequest
import com.tranquilai.ai.dto.response.AffirmationResponse
import com.tranquilai.ai.dto.response.GreetingResponse
import com.tranquilai.ai.dto.response.MoodInsightResponse
import com.tranquilai.ai.prompt.AiPrompts
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service

@Service
class InsightService(private val chatClient: ChatClient) {

    private val logger = LoggerFactory.getLogger(InsightService::class.java)

    fun generateGreeting(request: GenerateGreetingRequest): GreetingResponse {
        val prompt = AiPrompts.greetingPrompt(
            firstName = request.firstName,
            stressCauses = request.stressCauses,
            languageCode = request.languageCode,
        )
        val greeting = runCatching {
            chatClient.prompt().user(prompt).call().content()?.trim()
                ?: defaultGreeting(request.firstName)
        }.onFailure { logger.warn("Greeting generation failed: ${it.message}") }
         .getOrElse { defaultGreeting(request.firstName) }

        return GreetingResponse(greeting = greeting)
    }

    fun generateMoodInsight(request: MoodInsightRequest): MoodInsightResponse {
        val prompt = AiPrompts.moodInsightPrompt(
            moodScore = request.moodScore,
            emotions = request.emotions,
            notes = request.notes,
            stressCauses = request.stressCauses,
            languageCode = request.languageCode,
        )
        val insight = runCatching {
            chatClient.prompt().user(prompt).call().content()?.trim()
                ?: defaultMoodInsight(request.moodScore)
        }.onFailure { logger.warn("Mood insight failed: ${it.message}") }
         .getOrElse { defaultMoodInsight(request.moodScore) }

        return MoodInsightResponse(insight = insight)
    }

    fun generateAffirmation(request: GenerateAffirmationRequest): AffirmationResponse {
        val prompt = AiPrompts.affirmationPrompt(
            stressCauses = request.stressCauses,
            currentConcerns = request.currentConcerns,
            languageCode = request.languageCode,
        )
        val affirmation = runCatching {
            chatClient.prompt().user(prompt).call().content()?.trim()
                ?: "I am capable of handling whatever comes my way."
        }.onFailure { logger.warn("Affirmation generation failed: ${it.message}") }
         .getOrElse { "I am worthy of peace and joy." }

        return AffirmationResponse(affirmation = affirmation)
    }

    private fun defaultGreeting(firstName: String): String =
        "Hello${if (firstName != "there") ", $firstName" else ""}! How are you feeling today? 💙"

    private fun defaultMoodInsight(score: Int): String = when {
        score <= 3 -> "It sounds like today has been tough, and that's completely okay. 💙 Be gentle with yourself — difficult days are part of being human. Try a few slow, deep breaths to ground yourself in this moment."
        score <= 6 -> "You're navigating through a mixed day, and that takes strength. 🌟 Notice what small things have brought you comfort today. Every moment of awareness is a step toward feeling better."
        else -> "You're doing wonderfully! ☀️ That positive energy you're feeling is worth celebrating. Keep nurturing the habits and people that support your wellbeing."
    }
}
