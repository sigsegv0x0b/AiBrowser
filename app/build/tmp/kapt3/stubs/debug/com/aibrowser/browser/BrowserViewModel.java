package com.aibrowser.browser;

@kotlin.Metadata(mv = {2, 1, 0}, k = 1, xi = 48, d1 = {"\u0000@\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\b\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0007\u0018\u00002\u00020\u0001B\u0011\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0004\b\u0004\u0010\u0005J\u0010\u0010\u0012\u001a\u00020\u00132\b\b\u0002\u0010\u0014\u001a\u00020\u000fJ\u000e\u0010\u0015\u001a\u00020\u00132\u0006\u0010\u0016\u001a\u00020\u000fJ\u000e\u0010\u0017\u001a\u00020\u00132\u0006\u0010\u0016\u001a\u00020\u000fJ\b\u0010\u0018\u001a\u0004\u0018\u00010\tJ\u0010\u0010\u0019\u001a\u0004\u0018\u00010\t2\u0006\u0010\u0016\u001a\u00020\u000fJ\"\u0010\u001a\u001a\u00020\u00132\u0006\u0010\u0016\u001a\u00020\u000f2\u0012\u0010\u001b\u001a\u000e\u0012\u0004\u0012\u00020\t\u0012\u0004\u0012\u00020\t0\u001cJ\u0006\u0010\u001d\u001a\u00020\u0013R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0006\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\t0\b0\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010\n\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\t0\b0\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0016\u0010\u000e\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000f0\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010\u0010\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000f0\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\r\u00a8\u0006\u001e"}, d2 = {"Lcom/aibrowser/browser/BrowserViewModel;", "Landroidx/lifecycle/ViewModel;", "tabManager", "Lcom/aibrowser/browser/TabManager;", "<init>", "(Lcom/aibrowser/browser/TabManager;)V", "_tabs", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "Lcom/aibrowser/browser/TabState;", "tabs", "Lkotlinx/coroutines/flow/StateFlow;", "getTabs", "()Lkotlinx/coroutines/flow/StateFlow;", "_activeTabId", "", "activeTabId", "getActiveTabId", "createTab", "", "url", "closeTab", "id", "setActiveTab", "getActiveTab", "getTab", "updateTab", "update", "Lkotlin/Function1;", "refresh", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class BrowserViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.aibrowser.browser.TabManager tabManager = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.List<com.aibrowser.browser.TabState>> _tabs = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.aibrowser.browser.TabState>> tabs = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.String> _activeTabId = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.String> activeTabId = null;
    
    @javax.inject.Inject()
    public BrowserViewModel(@org.jetbrains.annotations.NotNull()
    com.aibrowser.browser.TabManager tabManager) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.aibrowser.browser.TabState>> getTabs() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.String> getActiveTabId() {
        return null;
    }
    
    public final void createTab(@org.jetbrains.annotations.NotNull()
    java.lang.String url) {
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
    
    public final void refresh() {
    }
}