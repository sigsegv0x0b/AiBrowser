package com.aibrowser.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.aibrowser.agent.StealthInjector
import com.aibrowser.browser.TabState
import java.net.URLEncoder

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContainer(
    tab: TabState,
    blockExternalIntents: Boolean,
    onIntentBlocked: (String) -> Unit,
    onTitleChanged: (String) -> Unit,
    onUrlChanged: (String) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onNavigationStateChanged: (Boolean, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            FrameLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                isFocusable = false
                isFocusableInTouchMode = false
                setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS)
            }
        },
        modifier = modifier,
        update = { wrapperView ->
            tab.webView?.let { tabWebView ->
                if (tabWebView.parent != wrapperView) {
                    tabWebView.parent?.let { (it as ViewGroup).removeView(tabWebView) }
                    wrapperView.removeAllViews()
                    tabWebView.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    wrapperView.addView(tabWebView)
                }

                tabWebView.webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        onUrlChanged(url ?: "")
                        onLoadingChanged(true)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        onUrlChanged(url ?: "")
                        onLoadingChanged(false)
                        view?.let { onNavigationStateChanged(it.canGoBack(), it.canGoForward()) }
                        CookieManager.getInstance().flush()
                        StealthInjector.inject(tabWebView)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        if (request?.isForMainFrame != true) return
                        val failingUrl = request.url?.toString() ?: return
                        if (failingUrl.startsWith("about:")) return
                        if (view == null) return
                        val host = request.url?.host.orEmpty()
                        val path = request.url?.path.orEmpty()
                        val query = (host + path).trimEnd('/').ifEmpty { failingUrl }
                        val searchUrl = "https://www.google.com/search?q=" +
                            URLEncoder.encode(query, "UTF-8")
                        val html = buildErrorPageHtml(query, searchUrl)
                        view.loadDataWithBaseURL("about:blank", html, "text/html", "UTF-8", null)
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val uri = request?.url ?: return false
                        val scheme = uri.scheme?.lowercase() ?: ""

                        if (scheme == "http" || scheme == "https" || !blockExternalIntents) {
                            return false
                        }

                        val blockedUrl = uri.toString()

                        if (scheme == "intent") {
                            val fallback = uri.getQueryParameter("browser_fallback_url")
                            if (!fallback.isNullOrBlank()) {
                                view?.loadUrl(fallback)
                            }
                            onIntentBlocked(blockedUrl)
                            return true
                        }

                        val host = uri.host ?: return true
                        val httpsUrl = buildString {
                            append("https://")
                            append(host)
                            if (uri.path != null) append(uri.path)
                            if (uri.query != null) append("?").append(uri.query)
                        }
                        view?.loadUrl(httpsUrl)
                        onIntentBlocked(blockedUrl)
                        return true
                    }
                }

                tabWebView.webChromeClient = object : WebChromeClient() {
                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        onTitleChanged(title ?: "Untitled")
                    }

                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        onLoadingChanged(newProgress < 100)
                    }
                }
            }
        }
    )
}

private fun htmlEscape(s: String): String = s
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&#39;")

private fun buildErrorPageHtml(query: String, searchUrl: String): String {
    val safeQuery = htmlEscape(query)
    val safeHref = htmlEscape(searchUrl)
    return """
        <!DOCTYPE html>
        <html>
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>This site can't be reached</title>
        <style>
        body { font-family: Roboto, sans-serif; margin: 32px 24px; color: #202124; background: #fff; line-height: 1.5; }
        h1 { font-size: 22px; margin: 0 0 12px; font-weight: 500; }
        .url { color: #5f6368; word-break: break-all; font-family: monospace; background: #f1f3f4; padding: 8px 12px; border-radius: 8px; margin: 12px 0; font-size: 13px; }
        p { margin: 8px 0; color: #3c4043; }
        .btn { display: inline-block; margin-top: 16px; padding: 12px 24px; background: #1a73e8; color: #fff; text-decoration: none; border-radius: 24px; font-weight: 500; font-size: 14px; }
        .btn:hover { background: #1765cc; }
        .muted { color: #5f6368; font-size: 13px; }
        </style>
        </head>
        <body>
        <h1>This site can't be reached</h1>
        <div class="url">$safeQuery</div>
        <p>The connection failed. Check the address for typos or try a Google search.</p>
        <a class="btn" href="$safeHref">Search Google for &quot;$safeQuery&quot;</a>
        <p class="muted" style="margin-top:24px">Tip: type a full URL (https://…) or a search query in the address bar.</p>
        </body>
        </html>
    """.trimIndent()
}
