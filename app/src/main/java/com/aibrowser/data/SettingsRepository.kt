package com.aibrowser.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aibrowser.data.models.ApiConfig
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
            baseUrl = prefs[KEY_BASE_URL] ?: provider.defaultBaseUrl
        )
    }

    suspend fun saveApiConfig(config: ApiConfig) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PROVIDER] = config.provider.name
            prefs[KEY_API_KEY] = config.apiKey
            prefs[KEY_MODEL] = config.model
            prefs[KEY_BASE_URL] = config.baseUrl
        }
    }
}
