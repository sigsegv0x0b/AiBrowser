package com.aibrowser.agent;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {2, 1, 0}, k = 1, xi = 48, d1 = {"\u0000h\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0007\b\u0007\u0018\u00002\u00020\u0001:\u0001+B\u0019\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0004\b\u0006\u0010\u0007J0\u0010\n\u001a\u00020\u000b2\f\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u000e0\r2\u0012\u0010\u000f\u001a\u000e\u0012\u0004\u0012\u00020\u0011\u0012\u0004\u0012\u00020\u00120\u0010H\u0086@\u00a2\u0006\u0002\u0010\u0013J\u0016\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u0017H\u0082@\u00a2\u0006\u0002\u0010\u0018J\u001e\u0010\u0019\u001a\u00020\u000b2\u0006\u0010\u001a\u001a\u00020\u001b2\f\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u000e0\rH\u0002J\u0018\u0010\u001c\u001a\u00020\u00172\u0006\u0010\u001a\u001a\u00020\u001b2\u0006\u0010\u001d\u001a\u00020\u000bH\u0002J\u0016\u0010\u001e\u001a\u00020\u000b2\u0006\u0010\u001a\u001a\u00020\u001bH\u0086@\u00a2\u0006\u0002\u0010\u001fJ\u001c\u0010 \u001a\b\u0012\u0004\u0012\u00020!0\r2\u0006\u0010\u001a\u001a\u00020\u001bH\u0086@\u00a2\u0006\u0002\u0010\u001fJ\u001f\u0010\"\u001a\u0004\u0018\u00010#2\u0006\u0010$\u001a\u00020%2\u0006\u0010&\u001a\u00020\u000bH\u0002\u00a2\u0006\u0002\u0010\'J$\u0010(\u001a\u00020\u000b2\u0006\u0010\u001d\u001a\u00020\u000b2\u0012\u0010\u000f\u001a\u000e\u0012\u0004\u0012\u00020\u0011\u0012\u0004\u0012\u00020\u00120\u0010H\u0002J$\u0010)\u001a\u00020\u000b2\u0006\u0010\u001d\u001a\u00020\u000b2\u0012\u0010\u000f\u001a\u000e\u0012\u0004\u0012\u00020\u0011\u0012\u0004\u0012\u00020\u00120\u0010H\u0002J$\u0010*\u001a\u00020\u000b2\u0006\u0010\u001d\u001a\u00020\u000b2\u0012\u0010\u000f\u001a\u000e\u0012\u0004\u0012\u00020\u0011\u0012\u0004\u0012\u00020\u00120\u0010H\u0002R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006,"}, d2 = {"Lcom/aibrowser/agent/AiService;", "", "client", "Lokhttp3/OkHttpClient;", "settingsRepository", "Lcom/aibrowser/data/SettingsRepository;", "<init>", "(Lokhttp3/OkHttpClient;Lcom/aibrowser/data/SettingsRepository;)V", "gson", "Lcom/google/gson/Gson;", "sendMessage", "", "messages", "", "Lcom/aibrowser/data/models/Message;", "onEvent", "Lkotlin/Function1;", "Lcom/aibrowser/agent/AiService$StreamEvent;", "", "(Ljava/util/List;Lkotlin/jvm/functions/Function1;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "executeWithRetry", "Lokhttp3/Response;", "request", "Lokhttp3/Request;", "(Lokhttp3/Request;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "buildRequestBody", "config", "Lcom/aibrowser/data/models/ApiConfig;", "buildRequest", "body", "testConnection", "(Lcom/aibrowser/data/models/ApiConfig;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "listModels", "Lcom/aibrowser/data/models/ModelInfo;", "parseNullableInt", "", "obj", "Lcom/google/gson/JsonObject;", "key", "(Lcom/google/gson/JsonObject;Ljava/lang/String;)Ljava/lang/Integer;", "parseStreamingResponse", "parseNonStreamingResponse", "parseSSEResponse", "StreamEvent", "app_debug"})
public final class AiService {
    @org.jetbrains.annotations.NotNull()
    private final okhttp3.OkHttpClient client = null;
    @org.jetbrains.annotations.NotNull()
    private final com.aibrowser.data.SettingsRepository settingsRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.google.gson.Gson gson = null;
    
    @javax.inject.Inject()
    public AiService(@org.jetbrains.annotations.NotNull()
    okhttp3.OkHttpClient client, @org.jetbrains.annotations.NotNull()
    com.aibrowser.data.SettingsRepository settingsRepository) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object sendMessage(@org.jetbrains.annotations.NotNull()
    java.util.List<com.aibrowser.data.models.Message> messages, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.aibrowser.agent.AiService.StreamEvent, kotlin.Unit> onEvent, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
    
    private final java.lang.Object executeWithRetry(okhttp3.Request request, kotlin.coroutines.Continuation<? super okhttp3.Response> $completion) {
        return null;
    }
    
    private final java.lang.String buildRequestBody(com.aibrowser.data.models.ApiConfig config, java.util.List<com.aibrowser.data.models.Message> messages) {
        return null;
    }
    
    private final okhttp3.Request buildRequest(com.aibrowser.data.models.ApiConfig config, java.lang.String body) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object testConnection(@org.jetbrains.annotations.NotNull()
    com.aibrowser.data.models.ApiConfig config, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object listModels(@org.jetbrains.annotations.NotNull()
    com.aibrowser.data.models.ApiConfig config, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.aibrowser.data.models.ModelInfo>> $completion) {
        return null;
    }
    
    private final java.lang.Integer parseNullableInt(com.google.gson.JsonObject obj, java.lang.String key) {
        return null;
    }
    
    private final java.lang.String parseStreamingResponse(java.lang.String body, kotlin.jvm.functions.Function1<? super com.aibrowser.agent.AiService.StreamEvent, kotlin.Unit> onEvent) {
        return null;
    }
    
    private final java.lang.String parseNonStreamingResponse(java.lang.String body, kotlin.jvm.functions.Function1<? super com.aibrowser.agent.AiService.StreamEvent, kotlin.Unit> onEvent) {
        return null;
    }
    
    private final java.lang.String parseSSEResponse(java.lang.String body, kotlin.jvm.functions.Function1<? super com.aibrowser.agent.AiService.StreamEvent, kotlin.Unit> onEvent) {
        return null;
    }
    
    @kotlin.Metadata(mv = {2, 1, 0}, k = 1, xi = 48, d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b6\u0018\u00002\u00020\u0001:\u0004\u0004\u0005\u0006\u0007B\t\b\u0004\u00a2\u0006\u0004\b\u0002\u0010\u0003\u0082\u0001\u0004\b\t\n\u000b\u00a8\u0006\f"}, d2 = {"Lcom/aibrowser/agent/AiService$StreamEvent;", "", "<init>", "()V", "Token", "ToolCallStart", "Done", "Error", "Lcom/aibrowser/agent/AiService$StreamEvent$Done;", "Lcom/aibrowser/agent/AiService$StreamEvent$Error;", "Lcom/aibrowser/agent/AiService$StreamEvent$Token;", "Lcom/aibrowser/agent/AiService$StreamEvent$ToolCallStart;", "app_debug"})
    public static abstract class StreamEvent {
        
        private StreamEvent() {
            super();
        }
        
        @kotlin.Metadata(mv = {2, 1, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0007\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u000f\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0004\b\u0004\u0010\u0005J\t\u0010\b\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\t\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\n\u001a\u00020\u000b2\b\u0010\f\u001a\u0004\u0018\u00010\rH\u00d6\u0003J\t\u0010\u000e\u001a\u00020\u000fH\u00d6\u0001J\t\u0010\u0010\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007\u00a8\u0006\u0011"}, d2 = {"Lcom/aibrowser/agent/AiService$StreamEvent$Done;", "Lcom/aibrowser/agent/AiService$StreamEvent;", "fullResponse", "", "<init>", "(Ljava/lang/String;)V", "getFullResponse", "()Ljava/lang/String;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "app_debug"})
        public static final class Done extends com.aibrowser.agent.AiService.StreamEvent {
            @org.jetbrains.annotations.NotNull()
            private final java.lang.String fullResponse = null;
            
            public Done(@org.jetbrains.annotations.NotNull()
            java.lang.String fullResponse) {
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String getFullResponse() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String component1() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.aibrowser.agent.AiService.StreamEvent.Done copy(@org.jetbrains.annotations.NotNull()
            java.lang.String fullResponse) {
                return null;
            }
            
            @java.lang.Override()
            public boolean equals(@org.jetbrains.annotations.Nullable()
            java.lang.Object other) {
                return false;
            }
            
            @java.lang.Override()
            public int hashCode() {
                return 0;
            }
            
            @java.lang.Override()
            @org.jetbrains.annotations.NotNull()
            public java.lang.String toString() {
                return null;
            }
        }
        
        @kotlin.Metadata(mv = {2, 1, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0007\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u000f\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0004\b\u0004\u0010\u0005J\t\u0010\b\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\t\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\n\u001a\u00020\u000b2\b\u0010\f\u001a\u0004\u0018\u00010\rH\u00d6\u0003J\t\u0010\u000e\u001a\u00020\u000fH\u00d6\u0001J\t\u0010\u0010\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007\u00a8\u0006\u0011"}, d2 = {"Lcom/aibrowser/agent/AiService$StreamEvent$Error;", "Lcom/aibrowser/agent/AiService$StreamEvent;", "message", "", "<init>", "(Ljava/lang/String;)V", "getMessage", "()Ljava/lang/String;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "app_debug"})
        public static final class Error extends com.aibrowser.agent.AiService.StreamEvent {
            @org.jetbrains.annotations.NotNull()
            private final java.lang.String message = null;
            
            public Error(@org.jetbrains.annotations.NotNull()
            java.lang.String message) {
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String getMessage() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String component1() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.aibrowser.agent.AiService.StreamEvent.Error copy(@org.jetbrains.annotations.NotNull()
            java.lang.String message) {
                return null;
            }
            
            @java.lang.Override()
            public boolean equals(@org.jetbrains.annotations.Nullable()
            java.lang.Object other) {
                return false;
            }
            
            @java.lang.Override()
            public int hashCode() {
                return 0;
            }
            
            @java.lang.Override()
            @org.jetbrains.annotations.NotNull()
            public java.lang.String toString() {
                return null;
            }
        }
        
        @kotlin.Metadata(mv = {2, 1, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0007\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u000f\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0004\b\u0004\u0010\u0005J\t\u0010\b\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\t\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\n\u001a\u00020\u000b2\b\u0010\f\u001a\u0004\u0018\u00010\rH\u00d6\u0003J\t\u0010\u000e\u001a\u00020\u000fH\u00d6\u0001J\t\u0010\u0010\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007\u00a8\u0006\u0011"}, d2 = {"Lcom/aibrowser/agent/AiService$StreamEvent$Token;", "Lcom/aibrowser/agent/AiService$StreamEvent;", "text", "", "<init>", "(Ljava/lang/String;)V", "getText", "()Ljava/lang/String;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "app_debug"})
        public static final class Token extends com.aibrowser.agent.AiService.StreamEvent {
            @org.jetbrains.annotations.NotNull()
            private final java.lang.String text = null;
            
            public Token(@org.jetbrains.annotations.NotNull()
            java.lang.String text) {
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String getText() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String component1() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.aibrowser.agent.AiService.StreamEvent.Token copy(@org.jetbrains.annotations.NotNull()
            java.lang.String text) {
                return null;
            }
            
            @java.lang.Override()
            public boolean equals(@org.jetbrains.annotations.Nullable()
            java.lang.Object other) {
                return false;
            }
            
            @java.lang.Override()
            public int hashCode() {
                return 0;
            }
            
            @java.lang.Override()
            @org.jetbrains.annotations.NotNull()
            public java.lang.String toString() {
                return null;
            }
        }
        
        @kotlin.Metadata(mv = {2, 1, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\r\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u001f\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0003\u00a2\u0006\u0004\b\u0006\u0010\u0007J\t\u0010\f\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\r\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u000e\u001a\u00020\u0003H\u00c6\u0003J\'\u0010\u000f\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\u0010\u001a\u00020\u00112\b\u0010\u0012\u001a\u0004\u0018\u00010\u0013H\u00d6\u0003J\t\u0010\u0014\u001a\u00020\u0015H\u00d6\u0001J\t\u0010\u0016\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\tR\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\tR\u0011\u0010\u0005\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\t\u00a8\u0006\u0017"}, d2 = {"Lcom/aibrowser/agent/AiService$StreamEvent$ToolCallStart;", "Lcom/aibrowser/agent/AiService$StreamEvent;", "id", "", "name", "args", "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", "getId", "()Ljava/lang/String;", "getName", "getArgs", "component1", "component2", "component3", "copy", "equals", "", "other", "", "hashCode", "", "toString", "app_debug"})
        public static final class ToolCallStart extends com.aibrowser.agent.AiService.StreamEvent {
            @org.jetbrains.annotations.NotNull()
            private final java.lang.String id = null;
            @org.jetbrains.annotations.NotNull()
            private final java.lang.String name = null;
            @org.jetbrains.annotations.NotNull()
            private final java.lang.String args = null;
            
            public ToolCallStart(@org.jetbrains.annotations.NotNull()
            java.lang.String id, @org.jetbrains.annotations.NotNull()
            java.lang.String name, @org.jetbrains.annotations.NotNull()
            java.lang.String args) {
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String getId() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String getName() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String getArgs() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String component1() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String component2() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String component3() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.aibrowser.agent.AiService.StreamEvent.ToolCallStart copy(@org.jetbrains.annotations.NotNull()
            java.lang.String id, @org.jetbrains.annotations.NotNull()
            java.lang.String name, @org.jetbrains.annotations.NotNull()
            java.lang.String args) {
                return null;
            }
            
            @java.lang.Override()
            public boolean equals(@org.jetbrains.annotations.Nullable()
            java.lang.Object other) {
                return false;
            }
            
            @java.lang.Override()
            public int hashCode() {
                return 0;
            }
            
            @java.lang.Override()
            @org.jetbrains.annotations.NotNull()
            public java.lang.String toString() {
                return null;
            }
        }
    }
}