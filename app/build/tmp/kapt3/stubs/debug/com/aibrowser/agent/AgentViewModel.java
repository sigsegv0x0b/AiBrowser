package com.aibrowser.agent;

@kotlin.Metadata(mv = {2, 1, 0}, k = 1, xi = 48, d1 = {"\u0000p\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0004\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0017\b\u0007\u0018\u0000 L2\u00020\u0001:\u0001LB3\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u0012\b\b\u0001\u0010\n\u001a\u00020\u000b\u00a2\u0006\u0004\b\f\u0010\rJ\b\u0010\'\u001a\u00020\u0011H\u0002J\u000e\u0010(\u001a\u00020)2\u0006\u0010*\u001a\u00020\u001cJ\u0018\u0010+\u001a\u00020)2\u0006\u0010,\u001a\u00020\u001c2\u0006\u0010-\u001a\u00020\u0011H\u0002J.\u0010.\u001a\u00020)2\u0006\u0010,\u001a\u00020\u001c2\u0006\u0010/\u001a\u00020\u001c2\u0006\u0010*\u001a\u00020\u001c2\f\u00100\u001a\b\u0012\u0004\u0012\u0002010\u0010H\u0002J \u00102\u001a\u00020)2\u0006\u0010,\u001a\u00020\u001c2\u0006\u0010*\u001a\u00020\u001c2\u0006\u00103\u001a\u00020\u001cH\u0002J,\u00104\u001a\u00020)2\u0006\u0010,\u001a\u00020\u001c2\u0006\u0010/\u001a\u00020\u001c2\u0006\u00105\u001a\u0002062\n\b\u0002\u00107\u001a\u0004\u0018\u00010\u001cH\u0002J\u0018\u00108\u001a\u00020)2\u0006\u0010,\u001a\u00020\u001c2\u0006\u00109\u001a\u00020\u0017H\u0002J\u0018\u0010:\u001a\u00020)2\u0006\u0010,\u001a\u00020\u001c2\u0006\u0010;\u001a\u00020\u0017H\u0002J\u0010\u0010<\u001a\u00020)2\u0006\u0010,\u001a\u00020\u001cH\u0002J\u0018\u0010=\u001a\u00020)2\u0006\u0010,\u001a\u00020\u001c2\u0006\u0010>\u001a\u00020\u001cH\u0002J\u0018\u0010?\u001a\u00020)2\u0006\u0010,\u001a\u00020\u001c2\u0006\u0010>\u001a\u00020\u001cH\u0002J$\u0010@\u001a\u00020)2\u0006\u0010,\u001a\u00020\u001c2\f\u00100\u001a\b\u0012\u0004\u0012\u0002010\u0010H\u0082@\u00a2\u0006\u0002\u0010AJ\u0016\u0010B\u001a\u00020)2\u0006\u0010,\u001a\u00020\u001cH\u0082@\u00a2\u0006\u0002\u0010CJ\u0006\u0010D\u001a\u00020)J\u000e\u0010E\u001a\u00020)2\u0006\u0010*\u001a\u00020\u001cJ\u0006\u0010F\u001a\u00020)J\u0012\u0010G\u001a\u00020)2\b\u0010,\u001a\u0004\u0018\u00010\u001cH\u0002J\u0010\u0010H\u001a\u00020)2\u0006\u0010,\u001a\u00020\u001cH\u0002J\u001a\u0010I\u001a\u00020)2\u0006\u0010\u0018\u001a\u00020\u00172\b\b\u0002\u0010>\u001a\u00020\u001cH\u0002J\u0016\u0010J\u001a\u00020\u001c2\u0006\u0010K\u001a\u00020\u001cH\u0086@\u00a2\u0006\u0002\u0010CR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u000e\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00110\u00100\u000fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010\u0012\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00110\u00100\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0015R\u0014\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00170\u000fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u00170\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0015R\u0014\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u00170\u000fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00170\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u0015R\u0014\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\u001c0\u000fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u001c0\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u0015R\u001a\u0010\u001f\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u001c0\u00100\u000fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010 \u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u001c0\u00100\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\b!\u0010\u0015R\u000e\u0010\"\u001a\u00020#X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010$\u001a\u0004\u0018\u00010\u001cX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010%\u001a\u00020\u001cX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010&\u001a\u00020\u001cX\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006M"}, d2 = {"Lcom/aibrowser/agent/AgentViewModel;", "Landroidx/lifecycle/ViewModel;", "aiService", "Lcom/aibrowser/agent/AiService;", "mcpController", "Lcom/aibrowser/agent/McpController;", "settingsRepository", "Lcom/aibrowser/data/SettingsRepository;", "tabManager", "Lcom/aibrowser/browser/TabManager;", "context", "Landroid/content/Context;", "<init>", "(Lcom/aibrowser/agent/AiService;Lcom/aibrowser/agent/McpController;Lcom/aibrowser/data/SettingsRepository;Lcom/aibrowser/browser/TabManager;Landroid/content/Context;)V", "_messages", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "Lcom/aibrowser/data/models/Message;", "messages", "Lkotlinx/coroutines/flow/StateFlow;", "getMessages", "()Lkotlinx/coroutines/flow/StateFlow;", "_isLoading", "", "isLoading", "_isPaused", "isPaused", "_currentAction", "", "currentAction", "getCurrentAction", "_actionHistory", "actionHistory", "getActionHistory", "gson", "Lcom/google/gson/Gson;", "currentTabId", "systemPromptContent", "ttsPromptContent", "createSystemMessage", "sendMessage", "", "content", "tabAppendMessage", "tabId", "message", "tabUpsertAssistantMessage", "id", "toolCalls", "Lcom/aibrowser/data/models/ToolCall;", "tabAppendToolResult", "toolCallId", "tabUpdateToolCallStatus", "status", "Lcom/aibrowser/data/models/ToolCall$ToolStatus;", "result", "tabSetLoading", "loading", "tabSetPaused", "paused", "syncFlowsFromTab", "tabSetCurrentAction", "action", "tabAppendActionHistory", "executeToolCalls", "(Ljava/lang/String;Ljava/util/List;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "sendFollowUp", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "pause", "resume", "clearChat", "saveToTab", "loadFromTab", "updateForegroundService", "generateTtsText", "text", "Companion", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class AgentViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.aibrowser.agent.AiService aiService = null;
    @org.jetbrains.annotations.NotNull()
    private final com.aibrowser.agent.McpController mcpController = null;
    @org.jetbrains.annotations.NotNull()
    private final com.aibrowser.data.SettingsRepository settingsRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.aibrowser.browser.TabManager tabManager = null;
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.List<com.aibrowser.data.models.Message>> _messages = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.aibrowser.data.models.Message>> messages = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> _isLoading = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isLoading = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> _isPaused = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isPaused = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.String> _currentAction = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.String> currentAction = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.List<java.lang.String>> _actionHistory = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<java.lang.String>> actionHistory = null;
    @org.jetbrains.annotations.NotNull()
    private final com.google.gson.Gson gson = null;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String currentTabId;
    @org.jetbrains.annotations.NotNull()
    private java.lang.String systemPromptContent = "You are a browser automation assistant. You have access to browser tools to help the user complete tasks. For every new page, first use browser_snapshot to understand the page content. After receiving tool results, always continue with the next action or provide a response to the user. Do not stop after a single tool call - keep analyzing and acting until the task is complete.";
    @org.jetbrains.annotations.NotNull()
    private java.lang.String ttsPromptContent = "You are a text-to-speech preprocessor. Rewrite the following text to be spoken aloud naturally. Remove all markdown formatting, code blocks, bullet points, numbered lists, links, URLs, and special characters. Keep only the plain conversational text. Make it flow naturally for speech. Keep it concise. Output ONLY the plain text, nothing else.";
    @org.jetbrains.annotations.NotNull()
    public static final com.aibrowser.agent.AgentViewModel.Companion Companion = null;
    
    @javax.inject.Inject()
    public AgentViewModel(@org.jetbrains.annotations.NotNull()
    com.aibrowser.agent.AiService aiService, @org.jetbrains.annotations.NotNull()
    com.aibrowser.agent.McpController mcpController, @org.jetbrains.annotations.NotNull()
    com.aibrowser.data.SettingsRepository settingsRepository, @org.jetbrains.annotations.NotNull()
    com.aibrowser.browser.TabManager tabManager, @dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.aibrowser.data.models.Message>> getMessages() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isLoading() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isPaused() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.String> getCurrentAction() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<java.lang.String>> getActionHistory() {
        return null;
    }
    
    private final com.aibrowser.data.models.Message createSystemMessage() {
        return null;
    }
    
    public final void sendMessage(@org.jetbrains.annotations.NotNull()
    java.lang.String content) {
    }
    
    private final void tabAppendMessage(java.lang.String tabId, com.aibrowser.data.models.Message message) {
    }
    
    private final void tabUpsertAssistantMessage(java.lang.String tabId, java.lang.String id, java.lang.String content, java.util.List<com.aibrowser.data.models.ToolCall> toolCalls) {
    }
    
    private final void tabAppendToolResult(java.lang.String tabId, java.lang.String content, java.lang.String toolCallId) {
    }
    
    private final void tabUpdateToolCallStatus(java.lang.String tabId, java.lang.String id, com.aibrowser.data.models.ToolCall.ToolStatus status, java.lang.String result) {
    }
    
    private final void tabSetLoading(java.lang.String tabId, boolean loading) {
    }
    
    private final void tabSetPaused(java.lang.String tabId, boolean paused) {
    }
    
    private final void syncFlowsFromTab(java.lang.String tabId) {
    }
    
    private final void tabSetCurrentAction(java.lang.String tabId, java.lang.String action) {
    }
    
    private final void tabAppendActionHistory(java.lang.String tabId, java.lang.String action) {
    }
    
    private final java.lang.Object executeToolCalls(java.lang.String tabId, java.util.List<com.aibrowser.data.models.ToolCall> toolCalls, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final java.lang.Object sendFollowUp(java.lang.String tabId, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    public final void pause() {
    }
    
    public final void resume(@org.jetbrains.annotations.NotNull()
    java.lang.String content) {
    }
    
    public final void clearChat() {
    }
    
    private final void saveToTab(java.lang.String tabId) {
    }
    
    private final void loadFromTab(java.lang.String tabId) {
    }
    
    private final void updateForegroundService(boolean isLoading, java.lang.String action) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object generateTtsText(@org.jetbrains.annotations.NotNull()
    java.lang.String text, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
    
    @kotlin.Metadata(mv = {2, 1, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010$\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\"\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00052\u0012\u0010\u0007\u001a\u000e\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\u00010\bJ\u0010\u0010\t\u001a\u0004\u0018\u00010\u00012\u0006\u0010\n\u001a\u00020\u000b\u00a8\u0006\f"}, d2 = {"Lcom/aibrowser/agent/AgentViewModel$Companion;", "", "<init>", "()V", "describeToolCall", "", "name", "args", "", "jsonToAny", "element", "Lcom/google/gson/JsonElement;", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String describeToolCall(@org.jetbrains.annotations.NotNull()
        java.lang.String name, @org.jetbrains.annotations.NotNull()
        java.util.Map<java.lang.String, ? extends java.lang.Object> args) {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.Object jsonToAny(@org.jetbrains.annotations.NotNull()
        com.google.gson.JsonElement element) {
            return null;
        }
    }
}