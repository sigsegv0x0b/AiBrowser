package com.aibrowser.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
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
