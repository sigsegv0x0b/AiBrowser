package com.aibrowser.browser;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {2, 1, 0}, k = 1, xi = 48, d1 = {"\u0000F\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0006\b\u0007\u0018\u00002\u00020\u0001B\u0013\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0004\b\u0004\u0010\u0005J\u0010\u0010\u0014\u001a\u00020\b2\b\b\u0002\u0010\u0015\u001a\u00020\u000fJ\u000e\u0010\u0016\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u000fJ\u000e\u0010\u0019\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u000fJ\b\u0010\u001a\u001a\u0004\u0018\u00010\bJ\u0010\u0010\u001b\u001a\u0004\u0018\u00010\b2\u0006\u0010\u0018\u001a\u00020\u000fJ\"\u0010\u001c\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u000f2\u0012\u0010\u001d\u001a\u000e\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\b0\u001eJ\u000e\u0010\u001f\u001a\u00020\u00172\u0006\u0010 \u001a\u00020\u000fJ\u000e\u0010!\u001a\u00020\u00172\u0006\u0010 \u001a\u00020\u000fJ\u000e\u0010\"\u001a\u00020\u00172\u0006\u0010 \u001a\u00020\u000fJ\u0016\u0010#\u001a\u00020\u00172\u0006\u0010 \u001a\u00020\u000f2\u0006\u0010\u0015\u001a\u00020\u000fR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\b0\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\t\u001a\b\u0012\u0004\u0012\u00020\b0\n8F\u00a2\u0006\u0006\u001a\u0004\b\u000b\u0010\fR\u0016\u0010\r\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000f0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010\u0010\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000f0\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013\u00a8\u0006$"}, d2 = {"Lcom/aibrowser/browser/TabManager;", "", "context", "Landroid/content/Context;", "<init>", "(Landroid/content/Context;)V", "_tabs", "", "Lcom/aibrowser/browser/TabState;", "tabs", "", "getTabs", "()Ljava/util/List;", "_activeTabId", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "activeTabId", "Lkotlinx/coroutines/flow/StateFlow;", "getActiveTabId", "()Lkotlinx/coroutines/flow/StateFlow;", "createTab", "url", "closeTab", "", "id", "setActiveTab", "getActiveTab", "getTab", "updateTab", "update", "Lkotlin/Function1;", "goBack", "tabId", "goForward", "reload", "loadUrl", "app_debug"})
public final class TabManager {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.aibrowser.browser.TabState> _tabs = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.String> _activeTabId = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.String> activeTabId = null;
    
    @javax.inject.Inject()
    public TabManager(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.aibrowser.browser.TabState> getTabs() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.String> getActiveTabId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.aibrowser.browser.TabState createTab(@org.jetbrains.annotations.NotNull()
    java.lang.String url) {
        return null;
    }
    
    public final void closeTab(@org.jetbrains.annotations.NotNull()
    java.lang.String id) {
    }
    
    public final void setActiveTab(@org.jetbrains.annotations.NotNull()
    java.lang.String id) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.aibrowser.browser.TabState getActiveTab() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.aibrowser.browser.TabState getTab(@org.jetbrains.annotations.NotNull()
    java.lang.String id) {
        return null;
    }
    
    public final void updateTab(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.aibrowser.browser.TabState, com.aibrowser.browser.TabState> update) {
    }
    
    public final void goBack(@org.jetbrains.annotations.NotNull()
    java.lang.String tabId) {
    }
    
    public final void goForward(@org.jetbrains.annotations.NotNull()
    java.lang.String tabId) {
    }
    
    public final void reload(@org.jetbrains.annotations.NotNull()
    java.lang.String tabId) {
    }
    
    public final void loadUrl(@org.jetbrains.annotations.NotNull()
    java.lang.String tabId, @org.jetbrains.annotations.NotNull()
    java.lang.String url) {
    }
}