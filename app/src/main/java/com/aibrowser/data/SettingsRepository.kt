package com.aibrowser.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.aibrowser.data.models.ApiConfig
import com.aibrowser.data.models.BehaviorConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_PROVIDER = stringPreferencesKey("api_provider")
        private val KEY_API_KEY = stringPreferencesKey("api_key")
        private val KEY_MODEL = stringPreferencesKey("model")
        private val KEY_BASE_URL = stringPreferencesKey("base_url")
        private val KEY_CONTEXT_SIZE = intPreferencesKey("context_size")
        private val KEY_MAX_OUTPUT = intPreferencesKey("max_output")
        private val KEY_SCROLL_INTO_VIEW = booleanPreferencesKey("scroll_into_view")
        private val KEY_TTS_PROMPT = stringPreferencesKey("tts_prompt")
        private val KEY_SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
    }

    val apiConfig: Flow<ApiConfig> = context.dataStore.data.map { prefs ->
        val providerName = prefs[KEY_PROVIDER] ?: ApiConfig.ApiProvider.OPENAI.name
        val provider = try {
            ApiConfig.ApiProvider.valueOf(providerName)
        } catch (_: Exception) {
            ApiConfig.ApiProvider.OPENAI
        }
        ApiConfig(
            provider = provider,
            apiKey = prefs[KEY_API_KEY] ?: "",
            model = prefs[KEY_MODEL] ?: provider.defaultModel,
            baseUrl = prefs[KEY_BASE_URL] ?: provider.defaultBaseUrl,
            contextSize = prefs[KEY_CONTEXT_SIZE] ?: 0,
            maxOutputTokens = prefs[KEY_MAX_OUTPUT] ?: 0
        )
    }

    suspend fun saveApiConfig(config: ApiConfig) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PROVIDER] = config.provider.name
            prefs[KEY_API_KEY] = config.apiKey
            prefs[KEY_MODEL] = config.model
            prefs[KEY_BASE_URL] = config.baseUrl
            if (config.contextSize > 0) prefs[KEY_CONTEXT_SIZE] = config.contextSize
            else prefs.remove(KEY_CONTEXT_SIZE)
            if (config.maxOutputTokens > 0) prefs[KEY_MAX_OUTPUT] = config.maxOutputTokens
            else prefs.remove(KEY_MAX_OUTPUT)
        }
    }

    val behaviorConfig: Flow<BehaviorConfig> = context.dataStore.data.map { prefs ->
        BehaviorConfig(
            scrollIntoView = prefs[KEY_SCROLL_INTO_VIEW] ?: true,
            ttsPrompt = prefs[KEY_TTS_PROMPT] ?: BehaviorConfig.DEFAULT_TTS_PROMPT,
            systemPrompt = prefs[KEY_SYSTEM_PROMPT] ?: BehaviorConfig.DEFAULT_SYSTEM_PROMPT
        )
    }

    suspend fun saveBehaviorConfig(config: BehaviorConfig) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SCROLL_INTO_VIEW] = config.scrollIntoView
            prefs[KEY_TTS_PROMPT] = config.ttsPrompt
            prefs[KEY_SYSTEM_PROMPT] = config.systemPrompt
        }
    }
}
