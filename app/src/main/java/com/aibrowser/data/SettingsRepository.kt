package com.aibrowser.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.aibrowser.agent.mnn.market.DownloadedMnnModel
import com.aibrowser.data.models.ApiConfig
import com.aibrowser.data.models.BehaviorConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
        private val KEY_BLOCK_EXTERNAL_INTENTS = booleanPreferencesKey("block_external_intents")
        private val KEY_TTS_PROMPT = stringPreferencesKey("tts_prompt")
        private val KEY_SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        private val KEY_NOTES_DIRECTORY_URI = stringPreferencesKey("notes_directory_uri")
        private val KEY_MNN_MODEL_PATH = stringPreferencesKey("mnn_model_path")
        private val KEY_MNN_BACKEND = stringPreferencesKey("mnn_backend")
        private val KEY_MNN_USE_MMAP = booleanPreferencesKey("mnn_use_mmap")
        private val KEY_MNN_DOWNLOADS = stringPreferencesKey("mnn_downloads")
        private val gson = Gson()
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

    suspend fun setProvider(provider: ApiConfig.ApiProvider) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PROVIDER] = provider.name
        }
    }

    val behaviorConfig: Flow<BehaviorConfig> = context.dataStore.data.map { prefs ->
        BehaviorConfig(
            scrollIntoView = prefs[KEY_SCROLL_INTO_VIEW] ?: true,
            blockExternalIntents = prefs[KEY_BLOCK_EXTERNAL_INTENTS] ?: true,
            ttsPrompt = prefs[KEY_TTS_PROMPT] ?: BehaviorConfig.DEFAULT_TTS_PROMPT,
            systemPrompt = prefs[KEY_SYSTEM_PROMPT] ?: BehaviorConfig.DEFAULT_SYSTEM_PROMPT
        )
    }

    suspend fun saveBehaviorConfig(config: BehaviorConfig) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SCROLL_INTO_VIEW] = config.scrollIntoView
            prefs[KEY_BLOCK_EXTERNAL_INTENTS] = config.blockExternalIntents
            prefs[KEY_TTS_PROMPT] = config.ttsPrompt
            prefs[KEY_SYSTEM_PROMPT] = config.systemPrompt
        }
    }

    val mnnModelPath: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_MNN_MODEL_PATH] ?: ""
    }

    suspend fun saveMnnModelPath(path: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MNN_MODEL_PATH] = path
        }
    }

    val mnnBackend: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_MNN_BACKEND] ?: "cpu"
    }

    suspend fun saveMnnBackend(backend: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MNN_BACKEND] = backend
        }
    }

    val mnnUseMmap: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_MNN_USE_MMAP] ?: false
    }

    suspend fun saveMnnUseMmap(enable: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MNN_USE_MMAP] = enable
        }
    }

    val notesDirectoryUri: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_NOTES_DIRECTORY_URI]
    }

    suspend fun saveNotesDirectoryUri(uri: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_NOTES_DIRECTORY_URI] = uri
        }
    }

    val downloadedModels: Flow<List<DownloadedMnnModel>> = context.dataStore.data.map { prefs ->
        val json = prefs[KEY_MNN_DOWNLOADS] ?: "[]"
        try {
            gson.fromJson(json, object : TypeToken<List<DownloadedMnnModel>>() {}.type)
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun saveDownloadedModel(model: DownloadedMnnModel) {
        context.dataStore.edit { prefs ->
            val json = prefs[KEY_MNN_DOWNLOADS] ?: "[]"
            val list: MutableList<DownloadedMnnModel> = try {
                gson.fromJson(json, object : TypeToken<List<DownloadedMnnModel>>() {}.type)
            } catch (_: Exception) {
                mutableListOf()
            }
            val existing = list.indexOfFirst { it.modelId == model.modelId }
            if (existing >= 0) list[existing] = model else list.add(model)
            prefs[KEY_MNN_DOWNLOADS] = gson.toJson(list)
        }
    }

    suspend fun removeDownloadedModel(modelId: String) {
        context.dataStore.edit { prefs ->
            val json = prefs[KEY_MNN_DOWNLOADS] ?: "[]"
            val list: MutableList<DownloadedMnnModel> = try {
                gson.fromJson(json, object : TypeToken<List<DownloadedMnnModel>>() {}.type)
            } catch (_: Exception) {
                mutableListOf()
            }
            list.removeAll { it.modelId == modelId }
            prefs[KEY_MNN_DOWNLOADS] = gson.toJson(list)
        }
    }

    suspend fun updateDownloadProgress(modelId: String, downloadedBytes: Long) {
        context.dataStore.edit { prefs ->
            val json = prefs[KEY_MNN_DOWNLOADS] ?: "[]"
            val list: MutableList<DownloadedMnnModel> = try {
                gson.fromJson(json, object : TypeToken<List<DownloadedMnnModel>>() {}.type)
            } catch (_: Exception) {
                mutableListOf()
            }
            val idx = list.indexOfFirst { it.modelId == modelId }
            if (idx >= 0 && !list[idx].complete) {
                list[idx] = list[idx].copy(downloadedBytes = downloadedBytes)
                prefs[KEY_MNN_DOWNLOADS] = gson.toJson(list)
            }
        }
    }

    suspend fun markDownloadComplete(modelId: String, totalBytes: Long) {
        context.dataStore.edit { prefs ->
            val json = prefs[KEY_MNN_DOWNLOADS] ?: "[]"
            val list: MutableList<DownloadedMnnModel> = try {
                gson.fromJson(json, object : TypeToken<List<DownloadedMnnModel>>() {}.type)
            } catch (_: Exception) {
                mutableListOf()
            }
            val idx = list.indexOfFirst { it.modelId == modelId }
            if (idx >= 0) {
                list[idx] = list[idx].copy(complete = true, downloadedBytes = totalBytes, totalBytes = totalBytes)
                prefs[KEY_MNN_DOWNLOADS] = gson.toJson(list)
            }
        }
    }
}
