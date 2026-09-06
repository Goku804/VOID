package com.core.voidapp.data.ai

/**
 * Supported AI providers. The provider is never hard-coded into the Chat
 * UI or the API client's caller — it's configured by the user under
 * SETTINGS -> INTEGRATIONS and read fresh from AIConfigStore on every call.
 */
enum class AIProvider(val displayName: String, val defaultBaseUrl: String, val defaultModel: String) {
    OPENAI("OpenAI", "https://api.openai.com/v1", "gpt-4o-mini"),
    ANTHROPIC("Anthropic (Claude)", "https://api.anthropic.com/v1", "claude-3-5-haiku-latest"),
    GROQ("Groq", "https://api.groq.com/openai/v1", "openai/gpt-oss-120b"),
    OPENAI_COMPATIBLE("Other (OpenAI-compatible endpoint)", "", "")
}

/** Everything needed to make one API call. Never persisted anywhere except AIConfigStore. */
data class AIConfig(
    val provider: AIProvider,
    val apiKey: String,
    val model: String,
    val baseUrl: String
)

/** Result of an AI request — no exceptions escape past AIRepository, callers just branch on this. */
sealed class AIResult {
    data class Success(val reply: String) : AIResult()
    data class Failure(val message: String) : AIResult()
}

/**
 * Result of a full tool-calling round trip: the model's final text answer
 * plus a plain-language log of every action it actually took along the
 * way (one entry per tool call, in order), so the caller can surface what
 * happened rather than silently applying changes.
 */
data class AIToolLoopResult(val finalText: String, val actionsLog: List<String>)

/** Thrown internally by the API client, always with a message safe to show the user. */
class AIException(message: String) : Exception(message)
