package com.aibrowser.browser;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {2, 1, 0}, k = 1, xi = 48, d1 = {"\u0000:\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0010\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0000\b\u0007\u0018\u00002\u00020\u0001B\u0013\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0004\b\u0004\u0010\u0005J\u0010\u0010\u0012\u001a\u00020\b2\b\b\u0002\u0010\u0013\u001a\u00020\u000eJ\u000e\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u000eJ\u000e\u0010\u0017\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u000eJ\b\u0010\u0018\u001a\u0004\u0018\u00010\bJ\u0010\u0010\u0019\u001a\u0004\u0018\u00010\b2\u0006\u0010\u0016\u001a\u00020\u000eJ\"\u0010\u001a\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u000e2\u0012\u0010\u001b\u001a\u000e\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\b0\u001cR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\b0\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\t\u001a\b\u0012\u0004\u0012\u00020\b0\n8F\u00a2\u0006\u0006\u001a\u0004\b\u000b\u0010\fR\u0010\u0010\r\u001a\u0004\u0018\u00010\u000eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0013\u0010\u000f\u001a\u0004\u0018\u00010\u000e8F\u00a2\u0006\u0006\u001a\u0004\b\u0010\u0010\u0011\u00a8\u0006\u001d"}, d2 = {"Lcom/aibrowser/browser/TabManager;", "", "context", "Landroid/content/Context;", "<init>", "(Landroid/content/Context;)V", "_tabs", "", "Lcom/aibrowser/browser/TabState;", "tabs", "", "getTabs", "()Ljava/util/List;", "_activeTabId", "", "activeTabId", "getActiveTabId", "()Ljava/lang/String;", "createTab", "url", "closeTab", "", "id", "setActiveTab", "getActiveTab", "getTab", "updateTab", "update", "Lkotlin/Function1;", "app_debug"})
public final class TabManager {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.aibrowser.browser.TabState> _tabs = null;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String _activeTabId;
    
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
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getActiveTabId() {
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
}