package com.aibrowser

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.aibrowser.agent.GenieXManager
import com.aibrowser.service.AgentForegroundService
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AiBrowserApp : Application() {
    @Inject lateinit var genieXManager: GenieXManager

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        CoroutineScope(Dispatchers.IO).launch {
            genieXManager.init()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                AgentForegroundService.CHANNEL_ID,
                "AI Agent",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when the AI agent is working in the background"
                setSound(null, null)
                enableVibration(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
