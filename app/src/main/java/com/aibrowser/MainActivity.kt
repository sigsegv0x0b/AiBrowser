package com.aibrowser

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.aibrowser.agent.AgentViewModel
import com.aibrowser.agent.AiService
import com.aibrowser.agent.LocalLlmProvider
import com.aibrowser.agent.LocalModelManager
import com.aibrowser.browser.BrowserViewModel
import com.aibrowser.data.SettingsRepository
import com.aibrowser.ui.navigation.NavGraph
import com.aibrowser.ui.theme.AiBrowserTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var aiService: AiService

    @Inject
    lateinit var localModelManager: LocalModelManager

    @Inject
    lateinit var localLlmProvider: LocalLlmProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AiBrowserTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val browserViewModel: BrowserViewModel = hiltViewModel()
                    val agentViewModel: AgentViewModel = hiltViewModel()
                    val isLoading by agentViewModel.isLoading.collectAsState()

                    LaunchedEffect(isLoading) {
                        if (isLoading) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }

                    NavGraph(
                        navController = navController,
                        browserViewModel = browserViewModel,
                        agentViewModel = agentViewModel,
                        settingsRepository = settingsRepository,
                        aiService = aiService,
                        localModelManager = localModelManager,
                        localLlmProvider = localLlmProvider
                    )
                }
            }
        }
    }
}
