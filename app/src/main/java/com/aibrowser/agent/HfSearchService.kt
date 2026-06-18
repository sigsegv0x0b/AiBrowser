package com.aibrowser.agent

import com.aibrowser.data.models.LLMModel
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class HfSearchResult(
    val models: List<LLMModel>,
    val hasMore: Boolean
)

@Singleton
class HfSearchService @Inject constructor() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "AiBrowser/1.0")
                .build()
            chain.proceed(request)
        }
        .build()

    private val gson = Gson()

    suspend fun searchGGUF(
        query: String,
        page: Int = 1,
        token: String? = null
    ): HfSearchResult = withContext(Dispatchers.IO) {
        val perPage = 20
        val url = buildString {
            append("https://huggingface.co/api/models?search=$query")
            append("&filter=gguf&sort=downloads&direction=-1")
            append("&limit=$perPage")
            if (page > 1) append("&page=$page")
        }

        val requestBuilder = Request.Builder().url(url)
        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        val response = client.newCall(requestBuilder.build()).execute()
        if (!response.isSuccessful) {
            return@withContext HfSearchResult(emptyList(), false)
        }

        val body = response.body?.string() ?: return@withContext HfSearchResult(emptyList(), false)
        val json = JsonParser.parseString(body)

        val models = if (json.isJsonArray) {
            json.asJsonArray.mapNotNull { el ->
                parseModel(el.asJsonObject)
            }
        } else if (json.isJsonObject && json.asJsonObject.has("items")) {
            json.asJsonObject.getAsJsonArray("items").mapNotNull { el ->
                parseModel(el.asJsonObject)
            }
        } else {
            emptyList()
        }

        HfSearchResult(models, models.size >= perPage)
    }

    private fun parseModel(obj: com.google.gson.JsonObject): LLMModel? {
        val modelId = obj.get("modelId")?.asString
            ?: obj.get("id")?.asString
            ?: return null

        val (author, name) = if (modelId.contains("/")) {
            val parts = modelId.split("/", limit = 2)
            parts[0] to parts[1]
        } else {
            "unknown" to modelId
        }

        val siblings = obj.getAsJsonArray("siblings") ?: return null
        val ggufFiles = siblings.mapNotNull { it.asJsonObject }
            .filter { it.get("rfilename")?.asString?.endsWith(".gguf") == true }

        if (ggufFiles.isEmpty()) return null

        val primaryFile = ggufFiles.first()
        val filename = primaryFile.get("rfilename")?.asString ?: return null

        val sizeBytes = primaryFile.get("size")?.asLong ?: 0L
        val sizeGb = sizeBytes.toFloat() / (1024 * 1024 * 1024)

        val displayName = name.replace("-", " ").replace("_", " ")
        val quant = extractQuant(filename)

        return LLMModel(
            id = modelId,
            repo = modelId,
            filename = filename,
            displayName = "$displayName ($quant)",
            sizeGb = sizeGb,
            quant = quant
        )
    }

    private fun extractQuant(filename: String): String {
        val patterns = listOf(
            "Q[0-9]_[0-9]", "Q[0-9]", "q[0-9]_[0-9]", "q[0-9]",
            "IQ[0-9]_[0-9A-Za-z]+", "IQ[0-9]",
            "F16", "F32", "BF16", "bf16", "fp16", "fp32"
        )
        for (pat in patterns) {
            val match = Regex(pat, RegexOption.IGNORE_CASE).find(filename)
            if (match != null) return match.value
        }
        return "unknown"
    }
}
