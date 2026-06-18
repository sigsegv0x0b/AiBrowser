package com.aibrowser.agent

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "HfSearchService"
private const val HF_API_BASE = "https://huggingface.co/api"

class HfSearchService(
    private val hfToken: String = ""
) {
    private val gson = Gson()

    private fun openConnection(urlStr: String): HttpURLConnection {
        var currentUrl = urlStr
        var redirectCount = 0
        val maxRedirects = 5

        while (redirectCount < maxRedirects) {
            val url = URL(currentUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", "AiBrowser/1.0")
                setRequestProperty("Accept", "application/json")
                if (hfToken.isNotEmpty()) {
                    setRequestProperty("Authorization", "Bearer $hfToken")
                }
            }

            val responseCode = connection.responseCode
            when {
                responseCode in 200..299 -> return connection
                responseCode in 301..308 -> {
                    val location = connection.getHeaderField("Location")
                    connection.disconnect()
                    currentUrl = URL(URL(currentUrl), location).toString()
                    redirectCount++
                }
                else -> {
                    val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: ""
                    connection.disconnect()
                    throw RuntimeException("HTTP $responseCode: $errorBody")
                }
            }
        }
        throw RuntimeException("Too many redirects")
    }

    suspend fun searchModels(query: String): List<HfModelSummary> = withContext(Dispatchers.IO) {
        val searchQuery = if ("gguf" in query.lowercase()) query else "$query gguf"
        val encoded = java.net.URLEncoder.encode(searchQuery, "UTF-8")
        val url = "$HF_API_BASE/models?" +
                "search=$encoded" +
                "&task=text-generation" +
                "&sort=downloads" +
                "&direction=-1" +
                "&limit=50"

        val connection = openConnection(url)
        try {
            val body = connection.inputStream.bufferedReader().readText()
            val listType = object : TypeToken<List<Map<String, Any>>>() {}.type
            val items: List<Map<String, Any>>? = gson.fromJson(body, listType)

            items?.filter { item ->
                val tags = (item["tags"] as? List<*>)?.map { it.toString() } ?: emptyList()
                "gguf" in tags
            }?.map { item ->
                HfModelSummary(
                    modelId = item["id"] as? String ?: "",
                    downloads = (item["downloads"] as? Double)?.toLong() ?: 0,
                    likes = (item["likes"] as? Double)?.toLong() ?: 0,
                    isGated = item["gated"] != null && item["gated"] != false,
                    trendingScore = (item["trendingScore"] as? Double)?.toInt() ?: 0
                )
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "searchModels failed for '$query': ${e.message}")
            throw e
        } finally {
            connection.disconnect()
        }
    }

    suspend fun getModelFiles(modelId: String): List<HfFileInfo> = withContext(Dispatchers.IO) {
        val url = "$HF_API_BASE/models/$modelId/tree/main"
        Log.d(TAG, "Fetching files: $url")

        val connection = openConnection(url)
        try {
            val body = connection.inputStream.bufferedReader().readText()
            val listType = object : TypeToken<List<Map<String, Any>>>() {}.type
            val items: List<Map<String, Any>>? = gson.fromJson(body, listType)

            items?.filter { item ->
                val path = item["path"] as? String ?: ""
                item["type"] == "file" && path.endsWith(".gguf")
            }?.mapNotNull { item ->
                val path = item["path"] as? String ?: return@mapNotNull null
                val lfs = item["lfs"] as? Map<*, *>
                val size = if (lfs != null) {
                    (lfs["size"] as? Double)?.toLong() ?: 0L
                } else {
                    (item["size"] as? Double)?.toLong() ?: 0L
                }
                HfFileInfo(filename = path, size = size)
            }?.sortedBy { it.size } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "getModelFiles failed for '$modelId': ${e.message}")
            throw e
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        fun extractQuantLabel(filename: String): String {
            val name = filename.removeSuffix(".gguf")
            val parts = name.split("-")
            val last = parts.lastOrNull()
            if (last != null && last.matches(Regex("^[QqIi][0-9BLK_MSX.]+$"))) return last
            val quantMatch = Regex("[Qq]\\d+[LKMSX_.]*|[Ii][Qq]\\d+[XS_.]*").find(name)
            return quantMatch?.value ?: last ?: name
        }

        fun buildDownloadUrl(modelId: String, filename: String): String {
            return "https://huggingface.co/$modelId/resolve/main/$filename"
        }

        fun formatSizeMb(bytes: Long): String {
            val mb = bytes.toDouble() / (1024.0 * 1024.0)
            return "${"%.0f".format(mb)} MB"
        }
    }
}
