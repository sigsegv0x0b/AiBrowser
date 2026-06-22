package com.aibrowser.browser

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewStructure
import android.view.autofill.AutofillValue
import android.webkit.WebView
import android.widget.FrameLayout

class AutofillWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_YES)
    }

    override fun autofill(value: AutofillValue) {
        super.autofill(value)
    }

    override fun onProvideAutofillVirtualStructure(structure: ViewStructure, flags: Int) {
        super.onProvideAutofillVirtualStructure(structure, flags)
    }
}

class AutofillFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        isFocusable = false
        isFocusableInTouchMode = false
        setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS)
    }

    override fun autofill(value: AutofillValue) {
        super.autofill(value)
        forwardAutofillToWebView(value)
    }

    override fun dispatchProvideAutofillStructure(structure: ViewStructure, flags: Int) {
        super.dispatchProvideAutofillStructure(structure, flags)
    }

    private fun forwardAutofillToWebView(value: AutofillValue) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is WebView) {
                child.autofill(value)
            }
        }
    }
}
