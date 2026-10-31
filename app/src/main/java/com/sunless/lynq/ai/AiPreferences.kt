package com.sunless.lynq.ai

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages persisted configuration for Featherless AI, including API key,
 * endpoint URL, LLM model choice, and preferred translation language.
 */
open class AiPreferences(context: Context) : AiConfigProvider {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val initialBaseUrl: String
    private val initialModel: String

    init {
        val storedUrl = prefs.getString(KEY_BASE_URL, null)
        initialBaseUrl = if (storedUrl.isNullOrBlank() || storedUrl.contains("api.featherless.ai")) {
            prefs.edit().putString(KEY_BASE_URL, DEFAULT_BASE_URL).apply()
            DEFAULT_BASE_URL
        } else {
            storedUrl
        }

        val storedModel = prefs.getString(KEY_MODEL, null)
        initialModel = if (storedModel.isNullOrBlank() || storedModel.contains("llama", ignoreCase = true)) {
            prefs.edit().putString(KEY_MODEL, DEFAULT_MODEL).apply()
            DEFAULT_MODEL
        } else {
            storedModel
        }
    }

    private val _apiKey = MutableStateFlow(prefs.getString(KEY_API_KEY, "") ?: "")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _baseUrl = MutableStateFlow(initialBaseUrl)
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

    private val _model = MutableStateFlow(initialModel)
    val model: StateFlow<String> = _model.asStateFlow()

    private val _targetLanguage = MutableStateFlow(prefs.getString(KEY_TARGET_LANG, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE)
    val targetLanguage: StateFlow<String> = _targetLanguage.asStateFlow()

    open fun setApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString(KEY_API_KEY, trimmed).apply()
        _apiKey.value = trimmed
    }

    open fun setBaseUrl(url: String) {
        val trimmed = url.trim().removeSuffix("/")
        val finalUrl = if (trimmed.isBlank()) DEFAULT_BASE_URL else trimmed
        prefs.edit().putString(KEY_BASE_URL, finalUrl).apply()
        _baseUrl.value = finalUrl
    }

    open fun setModel(modelName: String) {
        val trimmed = modelName.trim()
        val finalModel = if (trimmed.isBlank()) DEFAULT_MODEL else trimmed
        prefs.edit().putString(KEY_MODEL, finalModel).apply()
        _model.value = finalModel
    }

    open fun setTargetLanguage(languageName: String) {
        prefs.edit().putString(KEY_TARGET_LANG, languageName).apply()
        _targetLanguage.value = languageName
    }

    override fun getConfig(): AiConfig {
        return AiConfig(
            apiKey = _apiKey.value,
            baseUrl = _baseUrl.value,
            model = _model.value,
            targetLanguage = _targetLanguage.value,
        )
    }

    /**
     * Returns true if AI is configured and ready:
     * Either pointing to the cloud backend (which manages keys server-side),
     * or a direct client-side Featherless key has been supplied.
     */
    override fun hasApiKey(): Boolean = isBackendConfigured() || _apiKey.value.isNotBlank()

    fun isBackendConfigured(): Boolean = _baseUrl.value.isNotBlank() && !_baseUrl.value.contains("api.featherless.ai")

    companion object {
        private const val PREFS_NAME = "lynq_ai_prefs"
        private const val KEY_API_KEY = "featherless_api_key"
        private const val KEY_BASE_URL = "featherless_base_url"
        private const val KEY_MODEL = "featherless_model"
        private const val KEY_TARGET_LANG = "featherless_target_lang"

        const val DEFAULT_BASE_URL = "https://lynq-ai-backend.onrender.com"
        const val DEFAULT_MODEL = "Qwen/Qwen2.5-7B-Instruct"
        const val DEFAULT_LANGUAGE = "Hindi"

        val AVAILABLE_MODELS = listOf(
            "Qwen/Qwen2.5-7B-Instruct",
            "mistralai/Mistral-7B-Instruct-v0.3",
            "google/gemma-2-9b-it",
            "meta-llama/Meta-Llama-3.1-8B-Instruct",
        )
    }
}
