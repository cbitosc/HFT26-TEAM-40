package com.sunless.lynq.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data structures for Featherless AI summarization, translation, and triage classification.
 */

@Serializable
data class ThreadSummary(
    val overview: String,
    val triageLevel: String, // "CRITICAL", "HIGH", "NORMAL", "SAFE"
    val category: String, // "Medical", "Rescue/Trapped", "Fire", "Logistics", "Status Update"
    val keyPoints: List<String>,
    val actionableAdvice: String,
    val modelUsed: String,
    val language: String = "English",
    val isOfflineEstimate: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
)

@Serializable
data class BackendMessageItem(
    val sender: String,
    val body: String,
    val timestamp: Long? = null,
)

@Serializable
data class BackendSummarizeRequest(
    val peer_name: String = "Survivor",
    val messages: List<BackendMessageItem>,
    val target_language: String = "Hindi",
    val model: String? = null,
)

@Serializable
data class BackendTranslateRequest(
    val text: String,
    val target_language: String = "Hindi",
    val model: String? = null,
)

@Serializable
data class BackendTranslateResponse(
    val original: String = "",
    val translated: String = "",
    val target_language: String = "Hindi",
    val modelUsed: String = "",
)

data class SupportedLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flag: String,
) {
    val fullLabel: String get() = "$flag $displayName ($nativeName)"
}

object LanguageRegistry {
    val languages = listOf(
        SupportedLanguage("en", "English", "English", "🇺🇸"),
        SupportedLanguage("hi", "Hindi", "हिन्दी", "🇮🇳"),
        SupportedLanguage("es", "Spanish", "Español", "🇪🇸"),
        SupportedLanguage("fr", "French", "Français", "🇫🇷"),
        SupportedLanguage("de", "German", "Deutsch", "🇩🇪"),
        SupportedLanguage("ja", "Japanese", "日本語", "🇯🇵"),
        SupportedLanguage("ar", "Arabic", "العربية", "🇸🇦"),
        SupportedLanguage("bn", "Bengali", "বাংলা", "🇧🇩"),
    )

    fun find(nameOrCode: String): SupportedLanguage {
        return languages.firstOrNull {
            it.code.equals(nameOrCode, ignoreCase = true) ||
            it.displayName.equals(nameOrCode, ignoreCase = true)
        } ?: languages[0]
    }
}

data class AiConfig(
    val apiKey: String,
    val baseUrl: String,
    val model: String,
    val targetLanguage: String,
)

interface AiConfigProvider {
    fun getConfig(): AiConfig
    fun hasApiKey(): Boolean
}

// --- OpenAI API format for Featherless AI ---

@Serializable
data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Double = 0.2,
    @SerialName("max_tokens") val maxTokens: Int = 512,
)

@Serializable
data class OpenAiMessage(
    val role: String,
    val content: String,
)

@Serializable
data class OpenAiChatResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<OpenAiChoice> = emptyList(),
    val error: OpenAiError? = null,
)

@Serializable
data class OpenAiChoice(
    val index: Int = 0,
    val message: OpenAiMessage? = null,
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
data class OpenAiError(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null,
)
