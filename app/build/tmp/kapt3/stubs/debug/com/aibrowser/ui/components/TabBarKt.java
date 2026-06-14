package com.aibrowser.ui.components;

@kotlin.Metadata(mv = {2, 1, 0}, k = 2, xi = 48, d1 = {"\u00008\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0003\u001an\u0010\u0000\u001a\u00020\u00012\f\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\u0010\u0005\u001a\u0004\u0018\u00010\u00062\u0012\u0010\u0007\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\b2\u0012\u0010\t\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\b2\f\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00010\u000b2\f\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u00010\u000b2\b\b\u0002\u0010\r\u001a\u00020\u000eH\u0007\u001a4\u0010\u000f\u001a\u00020\u00012\u0006\u0010\u0010\u001a\u00020\u00062\u0006\u0010\u0011\u001a\u00020\u00122\f\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00010\u000b2\f\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00010\u000bH\u0003\u00a8\u0006\u0015"}, d2 = {"TabBar", "", "tabs", "", "Lcom/aibrowser/browser/TabState;", "activeTabId", "", "onTabClick", "Lkotlin/Function1;", "onCloseTab", "onNewTab", "Lkotlin/Function0;", "onSettingsClick", "modifier", "Landroidx/compose/ui/Modifier;", "TabChip", "title", "isActive", "", "onClick", "onClose", "app_debug"})
public final class TabBarKt {
    
    @androidx.compose.runtime.Composable()
    public static final void TabBar(@org.jetbrains.annotations.NotNull()
    java.util.List<com.aibrowser.browser.TabState> tabs, @org.jetbrains.annotations.Nullable()
    java.lang.String activeTabId, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onTabClick, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onCloseTab, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onNewTab, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onSettingsClick, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void TabChip(java.lang.String title, boolean isActive, kotlin.jvm.functions.Function0<kotlin.Unit> onClick, kotlin.jvm.functions.Function0<kotlin.Unit> onClose) {
    }
}