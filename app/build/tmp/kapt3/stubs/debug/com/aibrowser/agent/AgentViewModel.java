package com.aibrowser.agent;

@kotlin.Metadata(mv = {2, 1, 0}, k = 1, xi = 48, d1 = {"\u0000Z\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u0007\u0018\u0000 *2\u00020\u0001:\u0001*B\u0019\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0004\b\u0006\u0010\u0007J\u000e\u0010\u0017\u001a\u00020\u00182\u0006\u0010\u0019\u001a\u00020\u001aJ&\u0010\u001b\u001a\u00020\u00182\u0006\u0010\u001c\u001a\u00020\u001a2\u0006\u0010\u0019\u001a\u00020\u001a2\f\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u001e0\nH\u0002J\u001c\u0010\u001f\u001a\u00020\u00182\f\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u001e0\nH\u0082@\u00a2\u0006\u0002\u0010 J\u0006\u0010!\u001a\u00020\u0018J\u000e\u0010\"\u001a\u00020\u00182\u0006\u0010\u0019\u001a\u00020\u001aJ\u000e\u0010#\u001a\u00020\u0018H\u0082@\u00a2\u0006\u0002\u0010$J$\u0010%\u001a\u00020\u00182\u0006\u0010\u001c\u001a\u00020\u001a2\u0006\u0010&\u001a\u00020\'2\n\b\u0002\u0010(\u001a\u0004\u0018\u00010\u001aH\u0002J\u0006\u0010)\u001a\u00020\u0018R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\b\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000b0\n0\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010\f\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000b0\n0\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000fR\u0014\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00110\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00110\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u000fR\u0014\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00110\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00110\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u000fR\u000e\u0010\u0015\u001a\u00020\u0016X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006+"}, d2 = {"Lcom/aibrowser/agent/AgentViewModel;", "Landroidx/lifecycle/ViewModel;", "aiService", "Lcom/aibrowser/agent/AiService;", "mcpController", "Lcom/aibrowser/agent/McpController;", "<init>", "(Lcom/aibrowser/agent/AiService;Lcom/aibrowser/agent/McpController;)V", "_messages", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "Lcom/aibrowser/data/models/Message;", "messages", "Lkotlinx/coroutines/flow/StateFlow;", "getMessages", "()Lkotlinx/coroutines/flow/StateFlow;", "_isLoading", "", "isLoading", "_isPaused", "isPaused", "gson", "Lcom/google/gson/Gson;", "sendMessage", "", "content", "", "upsertAssistantMessage", "id", "toolCalls", "Lcom/aibrowser/data/models/ToolCall;", "executeToolCalls", "(Ljava/util/List;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "pause", "resume", "sendFollowUp", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updateToolCallStatus", "status", "Lcom/aibrowser/data/models/ToolCall$ToolStatus;", "result", "clearChat", "Companion", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class AgentViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.aibrowser.agent.AiService aiService = null;
    @org.jetbrains.annotations.NotNull()
    private final com.aibrowser.agent.McpController mcpController = null;
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
    private final com.google.gson.Gson gson = null;
    @org.jetbrains.annotations.NotNull()
    private static final com.aibrowser.data.models.Message SYSTEM_PROMPT = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.aibrowser.agent.AgentViewModel.Companion Companion = null;
    
    @javax.inject.Inject()
    public AgentViewModel(@org.jetbrains.annotations.NotNull()
    com.aibrowser.agent.AiService aiService, @org.jetbrains.annotations.NotNull()
    com.aibrowser.agent.McpController mcpController) {
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
    
    public final void sendMessage(@org.jetbrains.annotations.NotNull()
    java.lang.String content) {
    }
    
    private final void upsertAssistantMessage(java.lang.String id, java.lang.String content, java.util.List<com.aibrowser.data.models.ToolCall> toolCalls) {
    }
    
    private final java.lang.Object executeToolCalls(java.util.List<com.aibrowser.data.models.ToolCall> toolCalls, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    public final void pause() {
    }
    
    public final void resume(@org.jetbrains.annotations.NotNull()
    java.lang.String content) {
    }
    
    private final java.lang.Object sendFollowUp(kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final void updateToolCallStatus(java.lang.String id, com.aibrowser.data.models.ToolCall.ToolStatus status, java.lang.String result) {
    }
    
    public final void clearChat() {
    }
    
    @kotlin.Metadata(mv = {2, 1, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u0010\u0010\u0006\u001a\u0004\u0018\u00010\u00012\u0006\u0010\u0007\u001a\u00020\bR\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\t"}, d2 = {"Lcom/aibrowser/agent/AgentViewModel$Companion;", "", "<init>", "()V", "SYSTEM_PROMPT", "Lcom/aibrowser/data/models/Message;", "jsonToAny", "element", "Lcom/google/gson/JsonElement;", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.Object jsonToAny(@org.jetbrains.annotations.NotNull()
        com.google.gson.JsonElement element) {
            return null;
        }
    }
}