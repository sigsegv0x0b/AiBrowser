package com.aibrowser.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.aibrowser.agent.StealthInjector
import com.aibrowser.browser.TabState

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
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }
        },
        modifier = modifier,
        update = { wrapperView ->
            tab.webView?.let { tabWebView ->
                if (tabWebView.parent != wrapperView) {
                    tabWebView.parent?.let { (it as ViewGroup).removeView(tabWebView) }
                    wrapperView.removeAllViews()
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
                        StealthInjector.inject(tabWebView)
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
