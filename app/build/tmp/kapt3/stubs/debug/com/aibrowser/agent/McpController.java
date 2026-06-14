package com.aibrowser.agent;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {2, 1, 0}, k = 1, xi = 48, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0007\u0018\u00002\u00020\u0001B\u0011\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0004\b\u0004\u0010\u0005J\u0016\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\tH\u0086@\u00a2\u0006\u0002\u0010\u000bJ\u0012\u0010\f\u001a\u0004\u0018\u00010\u00012\u0006\u0010\r\u001a\u00020\u000eH\u0002J\u0014\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\t0\u00102\u0006\u0010\u0011\u001a\u00020\u0012R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0013"}, d2 = {"Lcom/aibrowser/agent/McpController;", "", "toolExecutor", "Lcom/aibrowser/agent/ToolExecutor;", "<init>", "(Lcom/aibrowser/agent/ToolExecutor;)V", "gson", "Lcom/google/gson/Gson;", "executeToolCall", "Lcom/aibrowser/data/models/ToolCall;", "toolCall", "(Lcom/aibrowser/data/models/ToolCall;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "jsonToAny", "element", "Lcom/google/gson/JsonElement;", "parseToolCalls", "", "responseContent", "", "app_debug"})
public final class McpController {
    @org.jetbrains.annotations.NotNull()
    private final com.aibrowser.agent.ToolExecutor toolExecutor = null;
    @org.jetbrains.annotations.NotNull()
    private final com.google.gson.Gson gson = null;
    
    @javax.inject.Inject()
    public McpController(@org.jetbrains.annotations.NotNull()
    com.aibrowser.agent.ToolExecutor toolExecutor) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object executeToolCall(@org.jetbrains.annotations.NotNull()
    com.aibrowser.data.models.ToolCall toolCall, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.aibrowser.data.models.ToolCall> $completion) {
        return null;
    }
    
    private final java.lang.Object jsonToAny(com.google.gson.JsonElement element) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.aibrowser.data.models.ToolCall> parseToolCalls(@org.jetbrains.annotations.NotNull()
    java.lang.String responseContent) {
        return null;
    }
}