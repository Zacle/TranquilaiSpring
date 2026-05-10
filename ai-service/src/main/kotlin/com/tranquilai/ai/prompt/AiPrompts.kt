package com.tranquilai.ai.prompt

object AiPrompts {

    private val languageNames = mapOf(
        "en" to "English", "es" to "Spanish", "fr" to "French", "de" to "German",
        "pt" to "Portuguese", "ar" to "Arabic", "zh" to "Chinese", "ja" to "Japanese",
        "ru" to "Russian", "ko" to "Korean", "hi" to "Hindi", "it" to "Italian",
    )

    private fun langInstruction(languageCode: String): String {
        val name = languageNames[languageCode] ?: "English"
        return if (languageCode != "en") "LANGUAGE REQUIREMENT: Respond exclusively in $name.\n\n" else ""
    }

    /**
     * Therapist system prompt — mirrors the mobile therapistSystemPrompt().
     * Used as the first system message for every chat conversation.
     */
    fun therapistSystemPrompt(languageCode: String = "en"): String {
        val lang = languageNames[languageCode] ?: "English"
        return """
${if (languageCode != "en") "You MUST respond exclusively in $lang." else ""}
You are a compassionate, professional mental wellness companion named Tranquil.
Your mission is to help the user feel better and boost their mood.

CORE PRINCIPLES:
- Mood boosting and emotional support is your top priority
- Respond with genuine empathy, warmth, and positivity
- Keep responses concise: 2-4 sentences maximum
- Use plain text only — NO markdown, NO bullet points, NO headers
- Add 1-2 relevant emojis naturally (💙 🌟 ✨ 😊 🤗 💪 🌈 🙏 💫 🌸 ☀️ 🍃)
- Suggest actionable techniques when appropriate (breathing, grounding, perspective shifts)
- Listen and validate feelings before offering solutions
- NEVER ask multiple questions in a single response
- If you ask a question, ask only ONE and make it open-ended
- Never diagnose, prescribe, or replace professional care
- If user expresses crisis or danger, gently encourage professional support
        """.trimIndent()
    }

    /**
     * Prompt to generate an opening greeting for a new conversation.
     */
    fun greetingPrompt(
        firstName: String,
        stressCauses: List<String> = emptyList(),
        languageCode: String = "en",
    ): String {
        val context = if (stressCauses.isNotEmpty())
            "The user has mentioned these stress factors: ${stressCauses.joinToString(", ")}." else ""
        return """
${langInstruction(languageCode)}
Generate a warm, personalized opening greeting for $firstName starting a mental wellness chat session.
$context
The greeting should:
- Address $firstName by name
- Feel welcoming and safe
- Gently invite them to share how they're feeling
- Be 1-2 sentences maximum
- Use plain text, 1 emoji naturally placed
- Be conversational, not clinical
        """.trimIndent()
    }

    /**
     * Mood insight prompt — 2-3 sentence supportive insight about a mood entry.
     */
    fun moodInsightPrompt(
        moodScore: Int,
        emotions: List<String>,
        notes: String?,
        stressCauses: List<String> = emptyList(),
        languageCode: String = "en",
    ): String {
        val emotionContext = if (emotions.isNotEmpty()) "Detected emotions: ${emotions.joinToString(", ")}." else ""
        val notesContext = if (!notes.isNullOrBlank()) "User's notes: \"$notes\"." else ""
        val stressContext = if (stressCauses.isNotEmpty()) "Known stress factors: ${stressCauses.joinToString(", ")}." else ""
        return """
${langInstruction(languageCode)}
The user logged a mood score of $moodScore out of 10. $emotionContext $notesContext $stressContext
Generate a 2-3 sentence supportive insight that:
- Acknowledges their current feeling with empathy
- Offers a gentle reframing or perspective
- Suggests one specific coping strategy relevant to their mood
- Uses plain text, no markdown, 1-2 emojis
- Feels warm and personal, not clinical
Return only the insight text, nothing else.
        """.trimIndent()
    }

    /**
     * Affirmation prompt — personalized first-person affirmation.
     */
    fun affirmationPrompt(
        stressCauses: List<String>,
        currentConcerns: List<String>,
        languageCode: String = "en",
    ): String {
        val stressCtx = if (stressCauses.isNotEmpty()) "Stress causes: ${stressCauses.joinToString(", ")}." else ""
        val concernsCtx = if (currentConcerns.isNotEmpty()) "Current concerns: ${currentConcerns.joinToString(", ")}." else ""
        return """
${langInstruction(languageCode)}
$stressCtx $concernsCtx
Generate a single personalised affirmation for the user. Requirements:
- Written in first person ("I am...", "I can...", "I choose...")
- 1-2 sentences maximum
- Directly relevant to their stress causes or concerns
- Uplifting, believable, and grounding
- Plain text only, no emojis
Return only the affirmation text, nothing else.
        """.trimIndent()
    }

    /**
     * Journal summarization prompt — returns structured JSON.
     * Mirrors mobile journalSummaryPrompt().
     */
    fun journalSummaryPrompt(
        promptText: String,
        content: String,
        category: String?,
        stressCauses: List<String> = emptyList(),
        currentConcerns: List<String> = emptyList(),
        languageCode: String = "en",
    ): String {
        val categoryCtx = category?.let { "Category: $it." } ?: ""
        val stressCtx = if (stressCauses.isNotEmpty()) "Known stress factors: ${stressCauses.joinToString(", ")}." else ""
        val concernsCtx = if (currentConcerns.isNotEmpty()) "Current concerns: ${currentConcerns.joinToString(", ")}." else ""
        return """
${langInstruction(languageCode)}
Analyze this journal entry and provide a structured summary.

Journal Prompt: "$promptText"
$categoryCtx
Entry Content:
"$content"
${if (stressCtx.isNotEmpty() || concernsCtx.isNotEmpty()) "\nUser Context:\n$stressCtx $concernsCtx" else ""}
Respond with ONLY a valid JSON object (no markdown, no extra text):
{
  "summary": "2-3 sentence warm, reflective summary addressing the user directly",
  "keyThemes": ["theme1", "theme2", "theme3"],
  "emotionalTone": "one of: positive | hopeful | neutral | processing | heavy | mixed",
  "suggestedFollowUp": "optional follow-up reflection question or null"
}

Requirements:
- summary: warm and reflective, use "you" to address the user
- keyThemes: 2-4 emotional themes or topics from the entry
- emotionalTone: pick the single best-fitting tone
- suggestedFollowUp: an open-ended question to deepen reflection, or null if not needed
        """.trimIndent()
    }

    /**
     * Conversation analysis prompts — mirror mobile's analyzeConversation() approach.
     */
    fun conversationTitlePrompt(messagesText: String, languageCode: String = "en"): String = """
${langInstruction(languageCode)}
Based on this conversation, generate a concise title (3-6 words) that captures the main topic or theme.
Return ONLY the title text, nothing else.

Conversation:
$messagesText
    """.trimIndent()

    fun conversationSummaryPrompt(messagesText: String, firstName: String, languageCode: String = "en"): String = """
${langInstruction(languageCode)}
Write a 2-3 sentence summary of this conversation between $firstName and their AI wellness companion.
Use $firstName's name naturally in the summary.
Be warm, reflective, and non-clinical.
Return ONLY the summary text, nothing else.

Conversation:
$messagesText
    """.trimIndent()

    fun conversationTopicsPrompt(messagesText: String, languageCode: String = "en"): String = """
${langInstruction(languageCode)}
List the 2-5 key topics or themes discussed in this conversation.
Return ONLY a comma-separated list of topics (e.g., "stress at work, breathing exercises, self-compassion").
No other text.

Conversation:
$messagesText
    """.trimIndent()

    fun conversationMoodPrompt(messagesText: String, languageCode: String = "en"): String = """
${langInstruction(languageCode)}
Analyze the user's mood at the start and end of this conversation.
Return ONLY a JSON object with exactly two fields:
{"moodAtStart": "very_low|low|neutral|good|great", "moodAtEnd": "very_low|low|neutral|good|great"}

Conversation:
$messagesText
    """.trimIndent()
}
