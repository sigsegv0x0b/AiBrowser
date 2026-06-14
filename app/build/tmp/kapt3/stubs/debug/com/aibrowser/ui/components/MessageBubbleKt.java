package com.aibrowser.ui.components;

@kotlin.Metadata(mv = {2, 1, 0}, k = 2, xi = 48, d1 = {"\u00002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\u001a\u001a\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u0005H\u0007\u001a\u0010\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\u0007H\u0002\u001a\u0010\u0010\f\u001a\u00020\u00012\u0006\u0010\b\u001a\u00020\u0007H\u0003\u001a\u0018\u0010\r\u001a\u00020\n2\u0006\u0010\b\u001a\u00020\u00072\u0006\u0010\u000e\u001a\u00020\nH\u0002\u001a\u001f\u0010\u000f\u001a\u00020\u00012\u0006\u0010\u0010\u001a\u00020\u00072\u0006\u0010\u0011\u001a\u00020\u0012H\u0003\u00a2\u0006\u0004\b\u0013\u0010\u0014\u001a\u001a\u0010\u0015\u001a\u00020\u00012\u0006\u0010\b\u001a\u00020\u00072\b\b\u0002\u0010\u0004\u001a\u00020\u0005H\u0007\u001a\u0010\u0010\u0016\u001a\u00020\u00172\u0006\u0010\b\u001a\u00020\u0007H\u0002\"\u000e\u0010\t\u001a\u00020\nX\u0082T\u00a2\u0006\u0002\n\u0000\"\u000e\u0010\u000b\u001a\u00020\nX\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0018"}, d2 = {"MessageBubble", "", "message", "Lcom/aibrowser/data/models/Message;", "modifier", "Landroidx/compose/ui/Modifier;", "stripMarkdown", "", "text", "CHARS_PER_SEC", "", "SKIP_SEC", "TtsSpeaker", "findTableEnd", "start", "CollapsibleToolContent", "content", "backgroundColor", "Landroidx/compose/ui/graphics/Color;", "CollapsibleToolContent-4WTKRHQ", "(Ljava/lang/String;J)V", "MarkdownText", "parseMarkdown", "Landroidx/compose/ui/text/AnnotatedString;", "app_debug"})
public final class MessageBubbleKt {
    private static final int CHARS_PER_SEC = 15;
    private static final int SKIP_SEC = 10;
    
    @androidx.compose.runtime.Composable()
    public static final void MessageBubble(@org.jetbrains.annotations.NotNull()
    com.aibrowser.data.models.Message message, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier) {
    }
    
    private static final java.lang.String stripMarkdown(java.lang.String text) {
        return null;
    }
    
    @androidx.compose.runtime.Composable()
    private static final void TtsSpeaker(java.lang.String text) {
    }
    
    private static final int findTableEnd(java.lang.String text, int start) {
        return 0;
    }
    
    @androidx.compose.runtime.Composable()
    public static final void MarkdownText(@org.jetbrains.annotations.NotNull()
    java.lang.String text, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier) {
    }
    
    private static final androidx.compose.ui.text.AnnotatedString parseMarkdown(java.lang.String text) {
        return null;
    }
}