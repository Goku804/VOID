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
    private const val KEY_API_KEYS = "api_keys"
    private const val KEY_MODEL = "model"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_TOOLS_ENABLED = "tools_enabled"

    /** Keys are stored newline-joined — never spaces/commas, since some providers' keys can contain those. */
    private const val KEY_DELIMITER = "\n"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(context: Context, config: AIConfig) {
        val keys = config.apiKeys.map { it.trim() }.filter { it.isNotBlank() }
            .ifEmpty { listOfNotNull(config.apiKey.trim().takeIf { it.isNotBlank() }) }
        prefs(context).edit()
            .putString(KEY_PROVIDER, config.provider.name)
            .putString(KEY_API_KEY, keys.firstOrNull() ?: "")
            .putString(KEY_API_KEYS, keys.joinToString(KEY_DELIMITER))
            .putString(KEY_MODEL, config.model)
            .putString(KEY_BASE_URL, config.baseUrl)
            .apply()
    }

    fun load(context: Context): AIConfig? {
        val p = prefs(context)
        val providerName = p.getString(KEY_PROVIDER, null) ?: return null
        val provider = runCatching { AIProvider.valueOf(providerName) }.getOrNull() ?: return null

        // Prefer the multi-key list; fall back to the single legacy key for
        // configs saved before multi-key support existed.
        val keysJoined = p.getString(KEY_API_KEYS, null)
        val keys = if (!keysJoined.isNullOrBlank()) {
            keysJoined.split(KEY_DELIMITER).map { it.trim() }.filter { it.isNotBlank() }
        } else {
            listOfNotNull(p.getString(KEY_API_KEY, null)?.trim()?.takeIf { it.isNotBlank() })
        }
        if (keys.isEmpty()) return null

        val model = p.getString(KEY_MODEL, provider.defaultModel) ?: provider.defaultModel
        val baseUrl = p.getString(KEY_BASE_URL, provider.defaultBaseUrl) ?: provider.defaultBaseUrl
        return AIConfig(provider = provider, apiKey = keys.first(), apiKeys = keys, model = model, baseUrl = baseUrl)
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    fun isConfigured(context: Context): Boolean = load(context) != null

    /**
     * Whether the AI is allowed to create/edit/delete VOID data via tool
     * calls (as opposed to read-only via AIContextBuilder). Granted by
     * default — the user can revoke it in Settings -> Integrations -> AI.
     */
    fun setToolsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_TOOLS_ENABLED, enabled).apply()
    }

    fun areToolsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_TOOLS_ENABLED, true)

    /** Never show the full key in UI — just enough to confirm which one is saved. */
    fun maskKey(key: String): String {
        if (key.length <= 8) return "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022"
        return "${key.take(4)}\u2022\u2022\u2022\u2022${key.takeLast(4)}"
    }
}
