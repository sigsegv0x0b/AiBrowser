package com.aibrowser.agent;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {2, 1, 0}, k = 1, xi = 48, d1 = {"\u0000F\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010$\n\u0002\b\u000b\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000b\n\u0002\b\b\n\u0002\u0010 \n\u0002\b\u0017\n\u0002\u0010\u0006\n\u0002\b\u0012\b\u0007\u0018\u00002\u00020\u0001B\u0019\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0004\b\u0006\u0010\u0007J*\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\t2\u0012\u0010\u000b\u001a\u000e\u0012\u0004\u0012\u00020\t\u0012\u0004\u0012\u00020\u00010\fH\u0086@\u00a2\u0006\u0002\u0010\rJ\u0016\u0010\u000e\u001a\u00020\t2\u0006\u0010\u000f\u001a\u00020\tH\u0082@\u00a2\u0006\u0002\u0010\u0010J\u0010\u0010\u0011\u001a\u00020\t2\u0006\u0010\u0012\u001a\u00020\tH\u0002J\u0010\u0010\u0013\u001a\u00020\t2\u0006\u0010\u0014\u001a\u00020\tH\u0002J\b\u0010\u0015\u001a\u00020\tH\u0002J,\u0010\u0016\u001a\u00020\t2\b\u0010\u0012\u001a\u0004\u0018\u00010\t2\b\u0010\u0017\u001a\u0004\u0018\u00010\u00182\b\u0010\u0019\u001a\u0004\u0018\u00010\u001aH\u0082@\u00a2\u0006\u0002\u0010\u001bJ\u0017\u0010\u001c\u001a\u00020\t2\b\u0010\u001d\u001a\u0004\u0018\u00010\u001aH\u0002\u00a2\u0006\u0002\u0010\u001eJD\u0010\u001f\u001a\u00020\t2\u0006\u0010\u0012\u001a\u00020\t2\b\u0010 \u001a\u0004\u0018\u00010\u001a2\b\u0010!\u001a\u0004\u0018\u00010\t2\u000e\u0010\"\u001a\n\u0012\u0004\u0012\u00020\t\u0018\u00010#2\b\b\u0002\u0010$\u001a\u00020\u001aH\u0082@\u00a2\u0006\u0002\u0010%J<\u0010&\u001a\u00020\t2\u0006\u0010\u0012\u001a\u00020\t2\u0006\u0010\'\u001a\u00020\t2\b\u0010(\u001a\u0004\u0018\u00010\u001a2\b\u0010)\u001a\u0004\u0018\u00010\u001a2\b\b\u0002\u0010$\u001a\u00020\u001aH\u0082@\u00a2\u0006\u0002\u0010*J2\u0010+\u001a\u00020\t2\u0018\u0010,\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\t\u0012\u0004\u0012\u00020\t0\f0#2\b\b\u0002\u0010$\u001a\u00020\u001aH\u0082@\u00a2\u0006\u0002\u0010-J.\u0010.\u001a\u00020\t2\u0006\u0010\u0012\u001a\u00020\t2\f\u0010/\u001a\b\u0012\u0004\u0012\u00020\t0#2\b\b\u0002\u0010$\u001a\u00020\u001aH\u0082@\u00a2\u0006\u0002\u00100J \u00101\u001a\u00020\t2\u0006\u0010\u0012\u001a\u00020\t2\b\b\u0002\u0010$\u001a\u00020\u001aH\u0082@\u00a2\u0006\u0002\u00102J\u0016\u00103\u001a\u00020\t2\u0006\u00104\u001a\u00020\tH\u0082@\u00a2\u0006\u0002\u0010\u0010J*\u00105\u001a\u00020\t2\u0006\u00106\u001a\u00020\t2\b\u0010\u0012\u001a\u0004\u0018\u00010\t2\b\b\u0002\u0010$\u001a\u00020\u001aH\u0082@\u00a2\u0006\u0002\u00107J,\u00108\u001a\u00020\t2\b\u0010\'\u001a\u0004\u0018\u00010\t2\b\u00109\u001a\u0004\u0018\u00010\t2\b\u0010:\u001a\u0004\u0018\u00010;H\u0082@\u00a2\u0006\u0002\u0010<J(\u0010=\u001a\u00020\t2\u0006\u0010>\u001a\u00020\t2\u0006\u0010?\u001a\u00020\t2\b\b\u0002\u0010$\u001a\u00020\u001aH\u0082@\u00a2\u0006\u0002\u00107J \u0010@\u001a\u00020\t2\u0006\u0010A\u001a\u00020\u001a2\b\u0010B\u001a\u0004\u0018\u00010\tH\u0082@\u00a2\u0006\u0002\u0010CJ\u0018\u0010D\u001a\u00020\t2\b\u0010E\u001a\u0004\u0018\u00010\tH\u0082@\u00a2\u0006\u0002\u0010\u0010J\u0018\u0010F\u001a\u00020\t2\u0006\u0010G\u001a\u00020\u00182\u0006\u0010H\u001a\u00020\u0018H\u0002J)\u0010I\u001a\u00020\t2\u0006\u0010J\u001a\u00020\t2\b\u0010K\u001a\u0004\u0018\u00010\u00182\b\u0010\u0014\u001a\u0004\u0018\u00010\tH\u0002\u00a2\u0006\u0002\u0010LR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006M"}, d2 = {"Lcom/aibrowser/agent/ToolExecutor;", "", "tabManager", "Lcom/aibrowser/browser/TabManager;", "settingsRepository", "Lcom/aibrowser/data/SettingsRepository;", "<init>", "(Lcom/aibrowser/browser/TabManager;Lcom/aibrowser/data/SettingsRepository;)V", "execute", "", "toolName", "arguments", "", "(Ljava/lang/String;Ljava/util/Map;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "runJs", "js", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "resolveSelector", "target", "navigate", "url", "navigateBack", "snapshot", "depth", "", "boxes", "", "(Ljava/lang/String;Ljava/lang/Integer;Ljava/lang/Boolean;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "screenshot", "fullPage", "(Ljava/lang/Boolean;)Ljava/lang/String;", "click", "doubleClick", "button", "modifiers", "", "scrollIntoView", "(Ljava/lang/String;Ljava/lang/Boolean;Ljava/lang/String;Ljava/util/List;ZLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "type", "text", "submit", "slowly", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Boolean;Ljava/lang/Boolean;ZLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "fillForm", "fields", "(Ljava/util/List;ZLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "selectOption", "values", "(Ljava/lang/String;Ljava/util/List;ZLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "hover", "(Ljava/lang/String;ZLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "pressKey", "key", "evaluate", "function", "(Ljava/lang/String;Ljava/lang/String;ZLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "waitFor", "textGone", "time", "", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Double;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "drag", "startTarget", "endTarget", "handleDialog", "accept", "promptText", "(ZLjava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "consoleMessages", "level", "resize", "width", "height", "tabs", "action", "index", "(Ljava/lang/String;Ljava/lang/Integer;Ljava/lang/String;)Ljava/lang/String;", "app_debug"})
public final class ToolExecutor {
    @org.jetbrains.annotations.NotNull()
    private final com.aibrowser.browser.TabManager tabManager = null;
    @org.jetbrains.annotations.NotNull()
    private final com.aibrowser.data.SettingsRepository settingsRepository = null;
    
    @javax.inject.Inject()
    public ToolExecutor(@org.jetbrains.annotations.NotNull()
    com.aibrowser.browser.TabManager tabManager, @org.jetbrains.annotations.NotNull()
    com.aibrowser.data.SettingsRepository settingsRepository) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object execute(@org.jetbrains.annotations.NotNull()
    java.lang.String toolName, @org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, ? extends java.lang.Object> arguments, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
    
    private final java.lang.Object runJs(java.lang.String js, kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
    
    private final java.lang.String resolveSelector(java.lang.String target) {
        return null;
    }
    
    private final java.lang.String navigate(java.lang.String url) {
        return null;
    }
    
    private final java.lang.String navigateBack() {
        return null;
    }
    
    private final java.lang.Object snapshot(java.lang.String target, java.lang.Integer depth, java.lang.Boolean boxes, kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
    
    private final java.lang.String screenshot(java.lang.Boolean fullPage) {
        return null;
    }
    
    private final java.lang.Object click(java.lang.String target, java.lang.Boolean doubleClick, java.lang.String button, java.util.List<java.lang.String> modifiers, boolean scrollIntoView, kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
    
    private final java.lang.Object type(java.lang.String target, java.lang.String text, java.lang.Boolean submit, java.lang.Boolean slowly, boolean scrollIntoView, kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
    
    private final java.lang.Object fillForm(java.util.List<? extends java.util.Map<java.lang.String, java.lang.String>> fields, boolean scrollIntoView, kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
    
    private final java.lang.Object selectOption(java.lang.String target, java.util.List<java.lang.String> values, boolean scrollIntoView, kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
    
    private final java.lang.Object hover(java.lang.String target, boolean scrollIntoView, kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
    
    private final java.lang.Object pressKey(java.lang.String key, kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
    
    private final java.lang.Object evaluate(java.lang.String function, java.lang.String target, boolean scrollIntoView, kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
    
    private final java.lang.Object waitFor(java.lang.String text, java.lang.String textGone, java.lang.Double time, kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
    
    private final java.lang.Object drag(java.lang.String startTarget, java.lang.String endTarget, boolean scrollIntoView, kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
    
    private final java.lang.Object handleDialog(boolean accept, java.lang.String promptText, kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
    
    private final java.lang.Object consoleMessages(java.lang.String level, kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
    
    private final java.lang.String resize(int width, int height) {
        return null;
    }
    
    private final java.lang.String tabs(java.lang.String action, java.lang.Integer index, java.lang.String url) {
        return null;
    }
}