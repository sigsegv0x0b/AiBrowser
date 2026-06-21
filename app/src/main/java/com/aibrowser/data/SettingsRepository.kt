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
import com.aibrowser.browser.PersistedTab
import com.aibrowser.data.models.ApiConfig
import com.aibrowser.data.models.BehaviorConfig
import com.aibrowser.data.models.CloudProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
        private val KEY_LOCATION_ENABLED = booleanPreferencesKey("location_enabled")
        private val KEY_TTS_PROMPT = stringPreferencesKey("tts_prompt")
        private val KEY_SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        private val KEY_NOTES_DIRECTORY_URI = stringPreferencesKey("notes_directory_uri")
        private val KEY_MNN_MODEL_PATH = stringPreferencesKey("mnn_model_path")
        private val KEY_MNN_BACKEND = stringPreferencesKey("mnn_backend")
        private val KEY_MNN_USE_MMAP = booleanPreferencesKey("mnn_use_mmap")
        private val KEY_MNN_PROMPT_CACHE = booleanPreferencesKey("mnn_prompt_cache")
        private val KEY_MNN_MAX_TOKENS = intPreferencesKey("mnn_max_tokens")
        private val KEY_MNN_TEMPERATURE = stringPreferencesKey("mnn_temperature")
        private val KEY_MNN_TOP_P = stringPreferencesKey("mnn_top_p")
        private val KEY_MNN_TOP_K = intPreferencesKey("mnn_top_k")
        private val KEY_MNN_PRECISION = stringPreferencesKey("mnn_precision")
        private val KEY_MNN_THREADS = intPreferencesKey("mnn_threads")
        private val KEY_MNN_DOWNLOADS = stringPreferencesKey("mnn_downloads")
        private val KEY_CLOUD_PROVIDERS = stringPreferencesKey("cloud_providers")
        private val KEY_ACTIVE_PROVIDER_ID = stringPreferencesKey("active_provider_id")
        private val KEY_CLOUD_MIGRATED = booleanPreferencesKey("cloud_migrated")
        private val KEY_TABS = stringPreferencesKey("tabs")
        private val gson = Gson()
    }

    val cloudProviders: Flow<List<CloudProvider>> = context.dataStore.data.map { prefs ->
        val json = prefs[KEY_CLOUD_PROVIDERS] ?: "[]"
        try {
            gson.fromJson(json, object : TypeToken<List<CloudProvider>>() {}.type)
        } catch (_: Exception) {
            emptyList()
        }
    }

    val activeProviderId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_PROVIDER_ID]
    }

    val apiConfig: Flow<ApiConfig> = combine(cloudProviders, activeProviderId) { providers, activeId ->
        if (activeId == null) {
            ApiConfig(provider = ApiConfig.ApiProvider.LOCAL_MNN)
        } else {
            val cp = providers.find { it.id == activeId }
            if (cp != null) {
                ApiConfig(
                    provider = cp.provider,
                    apiKey = cp.apiKey,
                    model = cp.model,
                    baseUrl = cp.baseUrl,
                    contextSize = cp.contextSize,
                    maxOutputTokens = cp.maxOutputTokens
                )
            } else {
                ApiConfig(provider = ApiConfig.ApiProvider.LOCAL_MNN)
            }
        }
    }

    suspend fun saveCloudProvider(provider: CloudProvider) {
        context.dataStore.edit { prefs ->
            val json = prefs[KEY_CLOUD_PROVIDERS] ?: "[]"
            val list: MutableList<CloudProvider> = try {
                gson.fromJson(json, object : TypeToken<List<CloudProvider>>() {}.type)
            } catch (_: Exception) {
                mutableListOf()
            }
            val existing = list.indexOfFirst { it.id == provider.id }
            if (existing >= 0) list[existing] = provider else list.add(provider)
            prefs[KEY_CLOUD_PROVIDERS] = gson.toJson(list)
        }
    }

    suspend fun deleteCloudProvider(id: String) {
        context.dataStore.edit { prefs ->
            val json = prefs[KEY_CLOUD_PROVIDERS] ?: "[]"
            val list: MutableList<CloudProvider> = try {
                gson.fromJson(json, object : TypeToken<List<CloudProvider>>() {}.type)
            } catch (_: Exception) {
                mutableListOf()
            }
            list.removeAll { it.id == id }
            prefs[KEY_CLOUD_PROVIDERS] = gson.toJson(list)
            if (prefs[KEY_ACTIVE_PROVIDER_ID] == id) {
                prefs.remove(KEY_ACTIVE_PROVIDER_ID)
            }
        }
    }

    suspend fun setActiveProvider(id: String?) {
        context.dataStore.edit { prefs ->
            if (id != null) prefs[KEY_ACTIVE_PROVIDER_ID] = id
            else prefs.remove(KEY_ACTIVE_PROVIDER_ID)
        }
    }

    suspend fun migrateCloudProvidersIfNeeded() {
        context.dataStore.edit { prefs ->
            if (prefs[KEY_CLOUD_MIGRATED] == true) return@edit
            prefs[KEY_CLOUD_MIGRATED] = true
            val providerName = prefs[KEY_PROVIDER] ?: ApiConfig.ApiProvider.OPENAI.name
            val provider = try {
                ApiConfig.ApiProvider.valueOf(providerName)
            } catch (_: Exception) {
                ApiConfig.ApiProvider.OPENAI
            }
            if (provider == ApiConfig.ApiProvider.LOCAL_MNN) return@edit
            val apiKey = prefs[KEY_API_KEY] ?: ""
            if (apiKey.isBlank()) return@edit
            val model = prefs[KEY_MODEL] ?: provider.defaultModel
            val baseUrl = prefs[KEY_BASE_URL] ?: provider.defaultBaseUrl
            val contextSize = prefs[KEY_CONTEXT_SIZE] ?: 0
            val maxOutput = prefs[KEY_MAX_OUTPUT] ?: 0
            val cp = CloudProvider(
                id = java.util.UUID.randomUUID().toString(),
                name = provider.displayName,
                provider = provider,
                apiKey = apiKey,
                model = model,
                baseUrl = baseUrl,
                contextSize = contextSize,
                maxOutputTokens = maxOutput
            )
            prefs[KEY_CLOUD_PROVIDERS] = gson.toJson(listOf(cp))
            prefs[KEY_ACTIVE_PROVIDER_ID] = cp.id
        }
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
            locationEnabled = prefs[KEY_LOCATION_ENABLED] ?: false,
            ttsPrompt = prefs[KEY_TTS_PROMPT] ?: BehaviorConfig.DEFAULT_TTS_PROMPT,
            systemPrompt = prefs[KEY_SYSTEM_PROMPT] ?: BehaviorConfig.DEFAULT_SYSTEM_PROMPT
        )
    }

    suspend fun saveBehaviorConfig(config: BehaviorConfig) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SCROLL_INTO_VIEW] = config.scrollIntoView
            prefs[KEY_BLOCK_EXTERNAL_INTENTS] = config.blockExternalIntents
            prefs[KEY_LOCATION_ENABLED] = config.locationEnabled
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

    val mnnPromptCache: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_MNN_PROMPT_CACHE] ?: true
    }

    suspend fun saveMnnPromptCache(enable: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MNN_PROMPT_CACHE] = enable
        }
    }

    val mnnMaxTokens: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_MNN_MAX_TOKENS] ?: 2048
    }

    suspend fun saveMnnMaxTokens(tokens: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MNN_MAX_TOKENS] = tokens
        }
    }

    val mnnTemperature: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_MNN_TEMPERATURE]?.toFloatOrNull() ?: 0.7f
    }

    suspend fun saveMnnTemperature(temp: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MNN_TEMPERATURE] = temp.toString()
        }
    }

    val mnnTopP: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_MNN_TOP_P]?.toFloatOrNull() ?: 0.95f
    }

    suspend fun saveMnnTopP(value: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MNN_TOP_P] = value.toString()
        }
    }

    val mnnTopK: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_MNN_TOP_K] ?: 20
    }

    suspend fun saveMnnTopK(value: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MNN_TOP_K] = value
        }
    }

    val mnnPrecision: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_MNN_PRECISION] ?: "low"
    }

    suspend fun saveMnnPrecision(value: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MNN_PRECISION] = value
        }
    }

    val mnnThreads: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_MNN_THREADS] ?: 4
    }

    suspend fun saveMnnThreads(count: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MNN_THREADS] = count
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

    val persistedTabs: Flow<List<PersistedTab>> = context.dataStore.data.map { prefs ->
        val json = prefs[KEY_TABS] ?: "[]"
        try {
            gson.fromJson(json, object : TypeToken<List<PersistedTab>>() {}.type)
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun saveTabs(tabs: List<PersistedTab>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TABS] = gson.toJson(tabs)
        }
    }
}
