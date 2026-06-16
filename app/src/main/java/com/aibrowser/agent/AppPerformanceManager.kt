package com.aibrowser.agent

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.os.PerformanceHintManager
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPerformanceManager @Inject constructor() {

    private var session: PerformanceHintManager.Session? = null
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null

    fun startPerformanceSession(context: Context, expectedDurationMs: Long) {
        stopPerformanceSession()

        val hintManager = context.getSystemService(PerformanceHintManager::class.java) ?: run {
            Log.w("AppPerfMgr", "PerformanceHintManager not available")
            return
        }

        handlerThread = HandlerThread("PerfHintThread", Process.THREAD_PRIORITY_FOREGROUND)
        handlerThread?.start()

        val threadIds = handlerThread?.let { intArrayOf(it.threadId) } ?: intArrayOf()

        try {
            session = hintManager.createHintSession(threadIds, expectedDurationMs)
            session?.updateTargetWorkDuration(expectedDurationMs)
            Log.d("AppPerfMgr", "Performance session started: ${expectedDurationMs}ms")
        } catch (e: Exception) {
            Log.w("AppPerfMgr", "Failed to create hint session: ${e.message}")
        }
    }

    fun reportProgress(durationMs: Long) {
        try {
            session?.reportActualWorkDuration(durationMs)
        } catch (_: Exception) { }
    }

    fun stopPerformanceSession() {
        handler = null
        try { session?.close() } catch (_: Exception) { }
        try { handlerThread?.quitSafely() } catch (_: Exception) { }
        session = null
        handlerThread = null
        Log.d("AppPerfMgr", "Performance session stopped")
    }
}
