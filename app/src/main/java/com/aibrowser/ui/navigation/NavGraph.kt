package com.aibrowser.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.aibrowser.agent.AgentViewModel
import com.aibrowser.agent.AiService
import com.aibrowser.agent.LlamaCppProvider
import com.aibrowser.agent.LocalLlmProvider
import com.aibrowser.agent.LocalModelManager
import com.aibrowser.agent.ModelDownloader
import com.aibrowser.browser.BrowserViewModel
import com.aibrowser.data.SettingsRepository
import com.aibrowser.ui.screens.AgentScreen
import com.aibrowser.ui.screens.BrowserScreen
import com.aibrowser.ui.screens.SettingsScreen

object Routes {
    const val BROWSER = "browser"
    const val AGENT = "agent"
    const val SETTINGS = "settings"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    browserViewModel: BrowserViewModel,
    agentViewModel: AgentViewModel,
    settingsRepository: SettingsRepository,
    aiService: AiService,
    localModelManager: LocalModelManager,
    localLlmProvider: LocalLlmProvider,
    llamaCppProvider: LlamaCppProvider,
    modelDownloader: ModelDownloader
) {
    NavHost(navController = navController, startDestination = Routes.BROWSER) {
        composable(Routes.BROWSER) {
            BrowserScreen(
                browserViewModel = browserViewModel,
                agentViewModel = agentViewModel,
                settingsRepository = settingsRepository,
                onNavigateToAgent = { navController.navigate(Routes.AGENT) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.AGENT) {
            AgentScreen(
                agentViewModel = agentViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                settingsRepository = settingsRepository,
                aiService = aiService,
                localModelManager = localModelManager,
                localLlmProvider = localLlmProvider,
                llamaCppProvider = llamaCppProvider,
                modelDownloader = modelDownloader,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
