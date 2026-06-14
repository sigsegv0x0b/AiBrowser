package com.aibrowser;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {2, 1, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u0007\u0018\u00002\u00020\u0001B\u0007\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u0012\u0010\u0010\u001a\u00020\u00112\b\u0010\u0012\u001a\u0004\u0018\u00010\u0013H\u0014R\u001e\u0010\u0004\u001a\u00020\u00058\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0006\u0010\u0007\"\u0004\b\b\u0010\tR\u001e\u0010\n\u001a\u00020\u000b8\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\f\u0010\r\"\u0004\b\u000e\u0010\u000f\u00a8\u0006\u0014"}, d2 = {"Lcom/aibrowser/MainActivity;", "Landroidx/activity/ComponentActivity;", "<init>", "()V", "settingsRepository", "Lcom/aibrowser/data/SettingsRepository;", "getSettingsRepository", "()Lcom/aibrowser/data/SettingsRepository;", "setSettingsRepository", "(Lcom/aibrowser/data/SettingsRepository;)V", "aiService", "Lcom/aibrowser/agent/AiService;", "getAiService", "()Lcom/aibrowser/agent/AiService;", "setAiService", "(Lcom/aibrowser/agent/AiService;)V", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "app_debug"})
public final class MainActivity extends androidx.activity.ComponentActivity {
    @javax.inject.Inject()
    public com.aibrowser.data.SettingsRepository settingsRepository;
    @javax.inject.Inject()
    public com.aibrowser.agent.AiService aiService;
    
    public MainActivity() {
        super(0);
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.aibrowser.data.SettingsRepository getSettingsRepository() {
        return null;
    }
    
    public final void setSettingsRepository(@org.jetbrains.annotations.NotNull()
    com.aibrowser.data.SettingsRepository p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.aibrowser.agent.AiService getAiService() {
        return null;
    }
    
    public final void setAiService(@org.jetbrains.annotations.NotNull()
    com.aibrowser.agent.AiService p0) {
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
}