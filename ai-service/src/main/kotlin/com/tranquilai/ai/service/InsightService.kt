package com.tranquilai.ai.service

import com.tranquilai.ai.dto.request.GenerateAffirmationRequest
import com.tranquilai.ai.dto.request.GenerateGreetingRequest
import com.tranquilai.ai.dto.request.MoodInsightRequest
import com.tranquilai.ai.dto.response.AffirmationResponse
import com.tranquilai.ai.dto.response.GreetingResponse
import com.tranquilai.ai.dto.response.MoodInsightResponse
import com.tranquilai.ai.prompt.AiPrompts
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service

@Service
class InsightService(
    private val chatClient: ChatClient,
    private val aiCallExecutor: AiCallExecutor,
) {

    fun generateGreeting(request: GenerateGreetingRequest): GreetingResponse {
        val prompt = AiPrompts.greetingPrompt(
            firstName = request.firstName,
            stressCauses = request.stressCauses,
            languageCode = request.languageCode,
        )
        val greeting = aiCallExecutor.execute(
            "greeting",
            fallback = { LocalizedFallbacks.greeting(request.firstName, request.languageCode) },
        ) {
            chatClient.prompt().user(prompt).call().content()?.trim()
                ?: LocalizedFallbacks.greeting(request.firstName, request.languageCode)
        }

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
        val insight = aiCallExecutor.execute(
            "mood insight",
            fallback = { LocalizedFallbacks.moodInsight(request.moodScore, request.languageCode) },
        ) {
            chatClient.prompt().user(prompt).call().content()?.trim()
                ?: LocalizedFallbacks.moodInsight(request.moodScore, request.languageCode)
        }

        return MoodInsightResponse(insight = insight)
    }

    fun generateAffirmation(request: GenerateAffirmationRequest): AffirmationResponse {
        val prompt = AiPrompts.affirmationPrompt(
            stressCauses = request.stressCauses,
            currentConcerns = request.currentConcerns,
            languageCode = request.languageCode,
        )
        val affirmation = aiCallExecutor.execute(
            "affirmation",
            fallback = { LocalizedFallbacks.affirmation(request.languageCode) },
        ) {
            chatClient.prompt().user(prompt).call().content()?.trim()
                ?: LocalizedFallbacks.affirmation(request.languageCode)
        }

        return AffirmationResponse(affirmation = affirmation)
    }
}
