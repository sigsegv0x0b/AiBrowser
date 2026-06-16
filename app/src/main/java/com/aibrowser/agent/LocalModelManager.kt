package com.aibrowser.agent

import android.content.Context
import android.net.wifi.WifiManager
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.EOFException
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLException

data class RemoteModel(
    val id: String,
    val displayName: String,
    val repo: String,
    val filename: String,
    val sizeMb: Int,
    val minRamGb: Int
)

@Singleton
class LocalModelManager @Inject constructor(
    private val client: OkHttpClient,
    @ApplicationContext private val context: Context
) {
    private val downloadClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "AiBrowser/1.0")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    val availableModels = listOf(
        RemoteModel("qwen3-0.6B-int4", "Qwen3 0.6B (498 MB)", "litert-community/Qwen3-0.6B", "qwen3_0_6b_mixed_int4.litertlm", 498, 2),
        RemoteModel("qwen2.5-1.5B-q8", "Qwen2.5 1.5B (1.5 GB)", "litert-community/Qwen2.5-1.5B-Instruct", "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm", 1524, 2),
        RemoteModel("qwen3-4B-int4", "Qwen3 4B (2.5 GB)", "litert-community/Qwen3-4B", "qwen3_4b_mixed_int4.litertlm", 2536, 4),
        RemoteModel("qwen3-4B-2507-int4", "Qwen3 4B 2507 (2.5 GB)", "litert-community/Qwen3-4B-Instruct-2507", "qwen3_4b_instruct_2507_mixed_int4.litertlm", 2536, 4),
        RemoteModel("qwen3-8B-int4", "Qwen3 8B (4.7 GB)", "litert-community/Qwen3-8B", "qwen3_8b_mixed_int4.litertlm", 4660, 6),
        RemoteModel("gemma-4-E2B-web", "Gemma 4 E2B Web (2.01 GB)", "litert-community/gemma-4-E2B-it-litert-lm", "gemma-4-E2B-it-web.litertlm", 2010, 4),
        RemoteModel("gemma-4-E2B", "Gemma 4 E2B (2.59 GB)", "litert-community/gemma-4-E2B-it-litert-lm", "gemma-4-E2B-it.litertlm", 2590, 6),
        RemoteModel("gemma-4-E4B-web", "Gemma 4 E4B Web (2.77 GB)", "litert-community/gemma-4-E4B-it-litert-lm", "gemma-4-E4B-it-web.litertlm", 2832, 6),
        RemoteModel("gemma-4-E4B", "Gemma 4 E4B (3.41 GB)", "litert-community/gemma-4-E4B-it-litert-lm", "gemma-4-E4B-it.litertlm", 3490, 8),
        RemoteModel("gemma-4-12B", "Gemma 4 12B (6.1 GB)", "litert-community/gemma-4-12B-it-litert-lm", "gemma-4-12B-it.litertlm", 6100, 10)
    )

    fun getModelDirectory(): File {
        val dir = File(context.filesDir, "litertlm_models")
        dir.mkdirs()
        return dir
    }

    fun getLocalModelFile(modelId: String): File {
        val model = availableModels.find { it.id == modelId } ?: return File(getModelDirectory(), modelId)
        return File(getModelDirectory(), model.filename)
    }

    fun isModelDownloaded(modelId: String): Boolean {
        return getLocalModelFile(modelId).exists()
    }

    fun getDownloadedModelPath(modelId: String): String? {
        val file = getLocalModelFile(modelId)
        return if (file.exists()) file.absolutePath else null
    }

    @Suppress("DEPRECATION")
    suspend fun downloadModel(
        modelId: String,
        onProgress: (Float, Float) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        val model = availableModels.find { it.id == modelId }
            ?: return@withContext Result.failure(IllegalArgumentException("Unknown model: $modelId"))

        val targetFile = getLocalModelFile(modelId)
        val tempFile = File(targetFile.absolutePath + ".part")

        if (targetFile.exists()) {
            return@withContext Result.success(targetFile.absolutePath)
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AiBrowser:modelDownload"
        ).apply {
            setReferenceCounted(false)
            acquire(10 * 60 * 1000L)
        }

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val wifiLock = wifiManager?.createWifiLock(
            WifiManager.WIFI_MODE_FULL_HIGH_PERF,
            "AiBrowser:modelDownload"
        )?.apply {
            setReferenceCounted(false)
            acquire()
        }

        try {
            var lastException: Exception? = null
            repeat(3) { attempt ->
                try {
                    val existingBytes = if (tempFile.exists()) tempFile.length() else 0L
                    val url = "https://huggingface.co/${model.repo}/resolve/main/${model.filename}"
                    val requestBuilder = Request.Builder().url(url)

                    if (existingBytes > 0) {
                        requestBuilder.header("Range", "bytes=$existingBytes-")
                    }

                    downloadClient.newCall(requestBuilder.build()).execute().use { response ->
                        if (!response.isSuccessful && response.code != 206) {
                            if (response.code == 416) {
                                tempFile.delete()
                                return@repeat
                            }
                            throw IOException("HTTP ${response.code}: ${response.message}")
                        }

                        val body = response.body ?: throw IOException("Empty response body")
                        val contentLength = if (response.code == 206) {
                            response.header("Content-Range")?.substringAfter("/")?.toLongOrNull()
                        } else {
                            body.contentLength().takeIf { it > 0 }
                        } ?: model.sizeMb.toLong() * 1024 * 1024

                        val inputStream = body.byteStream()
                        val raf = RandomAccessFile(tempFile, "rw")
                        if (existingBytes > 0) raf.seek(existingBytes)

                        val buffer = ByteArray(65536)
                        var bytesDownloaded = existingBytes
                        var lastReportedProgress = existingBytes.toFloat() / contentLength.toFloat()
                        var lastSpeedTime = System.currentTimeMillis()
                        var lastSpeedBytes = bytesDownloaded

                        try {
                            while (true) {
                                val read = inputStream.read(buffer)
                                if (read == -1) break
                                raf.write(buffer, 0, read)
                                bytesDownloaded += read

                                val now = System.currentTimeMillis()
                                val elapsed = (now - lastSpeedTime).coerceAtLeast(1)
                                val progress = bytesDownloaded.toFloat() / contentLength.toFloat()
                                val speedMBs = if (elapsed >= 1000) {
                                    val bytesInWindow = bytesDownloaded - lastSpeedBytes
                                    val speed = bytesInWindow.toFloat() / (elapsed / 1000f) / (1024f * 1024f)
                                    lastSpeedTime = now
                                    lastSpeedBytes = bytesDownloaded
                                    speed
                                } else {
                                    val instantSpeed = (bytesDownloaded - existingBytes).toFloat() /
                                        ((now - (lastSpeedTime - elapsed)).coerceAtLeast(1) / 1000f) / (1024f * 1024f)
                                    instantSpeed
                                }

                                if (progress - lastReportedProgress >= 0.01f || progress >= 1f) {
                                    onProgress(progress, speedMBs)
                                    lastReportedProgress = progress
                                }
                            }
                        } finally {
                            raf.close()
                            inputStream.close()
                        }

                        if (tempFile.length() < contentLength) {
                            throw IOException("Download incomplete: ${tempFile.length()} / $contentLength")
                        }

                        tempFile.renameTo(targetFile)
                        onProgress(1f, 0f)
                        return@withContext Result.success(targetFile.absolutePath)
                    }
                } catch (e: IOException) {
                    lastException = IOException(userMessage(e), e)
                    if (attempt < 2) {
                        val delay = when (e) {
                            is UnknownHostException -> 5000L
                            is SocketTimeoutException -> 4000L
                            else -> 2000L * (attempt + 1)
                        }
                        kotlinx.coroutines.delay(delay)
                    }
                }
            }

            Result.failure(lastException ?: IOException("Download failed after 3 attempts"))
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
            wifiLock?.let { if (it.isHeld) it.release() }
        }
    }

    fun getPartialDownloadSize(modelId: String): Long {
        val tempFile = File(getLocalModelFile(modelId).absolutePath + ".part")
        return if (tempFile.exists()) tempFile.length() else 0L
    }

    fun deleteModel(modelId: String): Boolean {
        val file = getLocalModelFile(modelId)
        return file.exists() && file.delete()
    }

    fun getModelSize(modelId: String): Long {
        return getLocalModelFile(modelId).length()
    }

    private fun userMessage(e: IOException): String {
        return when (e) {
            is SocketTimeoutException -> "Connection timed out. Check your internet and try again."
            is ConnectException -> "Could not reach server. Check your internet connection."
            is UnknownHostException -> "Network unavailable (screen may be off). Keep the app open and try again."
            is SSLException -> "Secure connection failed. Try again."
            is SocketException -> "Connection lost mid-download. Please retry — progress will resume."
            is EOFException -> "Download interrupted. Please retry — progress will resume."
            else -> {
                val msg = e.message ?: "Unknown error"
                when {
                    msg.startsWith("HTTP 401") -> "Access denied (401). This model may require login on huggingface.co."
                    msg.startsWith("HTTP 403") -> "Access forbidden (403). This model may require license acceptance."
                    msg.startsWith("HTTP 404") -> "Model file not found (404). The URL may have changed."
                    msg.startsWith("HTTP 5") -> "Server error. HuggingFace may be down — try again later."
                    msg.startsWith("HTTP") -> "Download error: $msg"
                    msg.contains("Download incomplete") -> "Download did not finish. Please retry — progress will resume."
                    else -> "Download error: $msg"
                }
            }
        }
    }
}
