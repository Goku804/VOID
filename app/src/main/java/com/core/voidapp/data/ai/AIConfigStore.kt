package com.core.voidapp.data.ai

import android.content.Context

/**
 * Local, on-device storage for AI provider configuration. Deliberately
 * kept OUT of Room/VoidRepository's in-memory state — credentials must
 * never end up in the same object graph as chat history, reports, or
 * anything that could later be exported. Lives in its own private
 * SharedPreferences file instead.
 *
 * The API key never appears in source code, is never logged, and is
 * never sent anywhere except directly to the configured provider's API
 * from AIApiClient.
 */
object AIConfigStore {
    private const val PREFS_NAME = "void_ai_config"
    private const val KEY_PROVIDER = "provider"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_MODEL = "model"
    private const val KEY_BASE_URL = "base_url"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(context: Context, config: AIConfig) {
        prefs(context).edit()
            .putString(KEY_PROVIDER, config.provider.name)
            .putString(KEY_API_KEY, config.apiKey)
            .putString(KEY_MODEL, config.model)
            .putString(KEY_BASE_URL, config.baseUrl)
            .apply()
    }

    fun load(context: Context): AIConfig? {
        val p = prefs(context)
        val providerName = p.getString(KEY_PROVIDER, null) ?: return null
        val provider = runCatching { AIProvider.valueOf(providerName) }.getOrNull() ?: return null
        val apiKey = p.getString(KEY_API_KEY, "") ?: ""
        if (apiKey.isBlank()) return null
        val model = p.getString(KEY_MODEL, provider.defaultModel) ?: provider.defaultModel
        val baseUrl = p.getString(KEY_BASE_URL, provider.defaultBaseUrl) ?: provider.defaultBaseUrl
        return AIConfig(provider = provider, apiKey = apiKey, model = model, baseUrl = baseUrl)
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    fun isConfigured(context: Context): Boolean = load(context) != null

    /** Never show the full key in UI — just enough to confirm which one is saved. */
    fun maskKey(key: String): String {
        if (key.length <= 8) return "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022"
        return "${key.take(4)}\u2022\u2022\u2022\u2022${key.takeLast(4)}"
    }
}
