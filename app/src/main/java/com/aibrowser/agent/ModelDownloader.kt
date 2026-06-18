package com.aibrowser.agent

import android.content.Context
import android.net.wifi.WifiManager
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
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

@Singleton
class ModelDownloader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val downloadClient = OkHttpClient.Builder()
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

    fun getModelDirectory(): File {
        val dir = File(context.filesDir, "gguf_models")
        dir.mkdirs()
        return dir
    }

    fun getLocalFile(repo: String, filename: String): File {
        val safeName = repo.replace("/", "__")
        return File(getModelDirectory(), "${safeName}__${filename}")
    }

    fun isDownloaded(repo: String, filename: String): Boolean {
        return getLocalFile(repo, filename).exists()
    }

    suspend fun downloadModel(
        repo: String,
        filename: String,
        token: String? = null,
        onProgress: (Float, Float) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        val targetFile = getLocalFile(repo, filename)
        val tempFile = File(targetFile.absolutePath + ".part")

        if (targetFile.exists()) {
            return@withContext Result.success(targetFile.absolutePath)
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AiBrowser:ggufDownload"
        ).apply {
            setReferenceCounted(false)
            acquire(10 * 60 * 1000L)
        }

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val wifiLock = wifiManager?.createWifiLock(
            WifiManager.WIFI_MODE_FULL_HIGH_PERF,
            "AiBrowser:ggufDownload"
        )?.apply {
            setReferenceCounted(false)
            acquire()
        }

        try {
            var lastException: Exception? = null
            repeat(3) { attempt ->
                try {
                    val existingBytes = if (tempFile.exists()) tempFile.length() else 0L
                    val url = "https://huggingface.co/$repo/resolve/main/$filename"

                    val requestBuilder = Request.Builder().url(url)
                    if (!token.isNullOrBlank()) {
                        requestBuilder.header("Authorization", "Bearer $token")
                    }
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
                        } ?: 0L

                        val inputStream = body.byteStream()
                        val raf = RandomAccessFile(tempFile, "rw")
                        if (existingBytes > 0) raf.seek(existingBytes)

                        val buffer = ByteArray(65536)
                        var bytesDownloaded = existingBytes
                        var lastReportedProgress = existingBytes.toFloat() / contentLength.toFloat().coerceAtLeast(1f)
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
                                val progress = if (contentLength > 0) bytesDownloaded.toFloat() / contentLength.toFloat() else 0f
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

                        if (contentLength > 0 && tempFile.length() < contentLength) {
                            throw IOException("Download incomplete: ${tempFile.length()} / $contentLength")
                        }

                        tempFile.renameTo(targetFile)
                        onProgress(1f, 0f)
                        return@withContext Result.success(targetFile.absolutePath)
                    }
                } catch (e: IOException) {
                    lastException = e
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

    fun deleteModel(repo: String, filename: String): Boolean {
        return getLocalFile(repo, filename).delete()
    }

    fun getModelSize(repo: String, filename: String): Long {
        return getLocalFile(repo, filename).length()
    }

    fun getPartialSize(repo: String, filename: String): Long {
        val tempFile = File(getLocalFile(repo, filename).absolutePath + ".part")
        return if (tempFile.exists()) tempFile.length() else 0L
    }

    fun listDownloadedModels(): List<File> {
        return getModelDirectory().listFiles()?.filter { it.isFile && it.name.endsWith(".gguf") } ?: emptyList()
    }
}
