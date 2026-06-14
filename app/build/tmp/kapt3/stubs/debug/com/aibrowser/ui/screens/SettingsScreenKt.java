package com.aibrowser.ui.screens;

@kotlin.Metadata(mv = {2, 1, 0}, k = 2, xi = 48, d1 = {"\u0000<\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\u001a&\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00010\u0007H\u0007\u001a0\u0010\b\u001a\u00020\u00012\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000eH\u0003\u001a$\u0010\u000f\u001a\u00020\u00012\u0006\u0010\u0010\u001a\u00020\u00112\u0012\u0010\u0012\u001a\u000e\u0012\u0004\u0012\u00020\u0011\u0012\u0004\u0012\u00020\u00010\u0013H\u0003\u00a8\u0006\u0014"}, d2 = {"SettingsScreen", "", "settingsRepository", "Lcom/aibrowser/data/SettingsRepository;", "aiService", "Lcom/aibrowser/agent/AiService;", "onBack", "Lkotlin/Function0;", "LlmSettingsTab", "config", "Lcom/aibrowser/data/models/ApiConfig;", "scope", "Lkotlinx/coroutines/CoroutineScope;", "focusManager", "Landroidx/compose/ui/focus/FocusManager;", "BehaviorSettingsTab", "behavior", "Lcom/aibrowser/data/models/BehaviorConfig;", "onSave", "Lkotlin/Function1;", "app_debug"})
public final class SettingsScreenKt {
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void SettingsScreen(@org.jetbrains.annotations.NotNull()
    com.aibrowser.data.SettingsRepository settingsRepository, @org.jetbrains.annotations.NotNull()
    com.aibrowser.agent.AiService aiService, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onBack) {
    }
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    private static final void LlmSettingsTab(com.aibrowser.data.models.ApiConfig config, com.aibrowser.agent.AiService aiService, com.aibrowser.data.SettingsRepository settingsRepository, kotlinx.coroutines.CoroutineScope scope, androidx.compose.ui.focus.FocusManager focusManager) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void BehaviorSettingsTab(com.aibrowser.data.models.BehaviorConfig behavior, kotlin.jvm.functions.Function1<? super com.aibrowser.data.models.BehaviorConfig, kotlin.Unit> onSave) {
    }
}