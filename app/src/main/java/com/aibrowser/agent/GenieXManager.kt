package com.aibrowser.agent

import android.content.Context
import android.util.Log
import com.geniex.sdk.GenieXSdk
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class GenieXManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var initialized = false

    suspend fun init() {
        if (initialized) return
        Log.i(TAG, "Initializing GenieX SDK...")
        suspendCancellableCoroutine<Unit> { cont ->
            GenieXSdk.getInstance().init(context, object : GenieXSdk.InitCallback {
                override fun onSuccess() {
                    initialized = true
                    Log.i(TAG, "GenieX SDK initialized")
                    cont.resume(Unit)
                }
                override fun onFailure(error: String) {
                    Log.e(TAG, "GenieX SDK init failed: $error")
                    cont.resume(Unit)
                }
            })
        }
    }

    fun isInitialized(): Boolean = initialized

    companion object {
        private const val TAG = "GenieXManager"
    }
}
