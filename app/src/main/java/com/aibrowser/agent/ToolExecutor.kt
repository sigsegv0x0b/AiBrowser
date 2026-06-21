package com.aibrowser.agent

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.aibrowser.browser.TabManager
import com.aibrowser.data.SettingsRepository
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolExecutor @Inject constructor(
    private val tabManager: TabManager,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context
) {
    suspend fun execute(toolName: String, arguments: Map<String, Any>): String {
        val config = settingsRepository.behaviorConfig.first()
        val scrollIntoView = config.scrollIntoView
        return withContext(Dispatchers.Main) {
            when (toolName) {
                "browser_navigate" -> navigate(arguments["url"] as String)
                "browser_navigate_back" -> navigateBack()
                "browser_snapshot" -> snapshot(
                    target = arguments["target"] as? String,
                    depth = (arguments["depth"] as? Double)?.toInt(),
                    boxes = arguments["boxes"] as? Boolean
                )
                "browser_take_screenshot" -> screenshot(
                    fullPage = arguments["fullPage"] as? Boolean
                )
                "browser_click" -> click(
                    target = arguments["target"] as String,
                    doubleClick = arguments["doubleClick"] as? Boolean,
                    button = arguments["button"] as? String,
                    modifiers = arguments["modifiers"] as? List<String>,
                    scrollIntoView = scrollIntoView
                )
                "browser_type" -> type(
                    target = arguments["target"] as String,
                    text = arguments["text"] as String,
                    submit = arguments["submit"] as? Boolean,
                    slowly = arguments["slowly"] as? Boolean,
                    scrollIntoView = scrollIntoView
                )
                "browser_fill_form" -> fillForm(
                    fields = parseFillFormFields(arguments["fields"]),
                    scrollIntoView = scrollIntoView
                )
                "browser_select_option" -> selectOption(
                    target = arguments["target"] as String,
                    values = arguments["values"] as List<String>,
                    scrollIntoView = scrollIntoView
                )
                "browser_hover" -> hover(
                    target = arguments["target"] as String,
                    scrollIntoView = scrollIntoView
                )
                "browser_press_key" -> pressKey(arguments["key"] as String)
                "browser_evaluate" -> evaluate(
                    function = arguments["function"] as String,
                    target = arguments["target"] as? String,
                    scrollIntoView = scrollIntoView
                )
                "browser_wait_for" -> waitFor(
                    text = arguments["text"] as? String,
                    textGone = arguments["textGone"] as? String,
                    time = arguments["time"] as? Double
                )
                "browser_tabs" -> tabs(
                    action = arguments["action"] as String,
                    index = (arguments["index"] as? Double)?.toInt(),
                    url = arguments["url"] as? String
                )
                "browser_drag" -> drag(
                    startTarget = arguments["startTarget"] as String,
                    endTarget = arguments["endTarget"] as String,
                    scrollIntoView = scrollIntoView
                )
                "browser_handle_dialog" -> handleDialog(
                    accept = arguments["accept"] as? Boolean ?: true,
                    promptText = arguments["promptText"] as? String
                )
                "browser_console_messages" -> consoleMessages(
                    level = arguments["level"] as? String
                )
                "browser_resize" -> resize(
                    width = (arguments["width"] as? Double)?.toInt() ?: 0,
                    height = (arguments["height"] as? Double)?.toInt() ?: 0
                )
                "file_read" -> fileRead(
                    path = arguments["path"] as String,
                    offset = (arguments["offset"] as? Double)?.toInt() ?: -1,
                    length = (arguments["length"] as? Double)?.toInt() ?: -1
                )
                "file_write" -> fileWrite(
                    path = arguments["path"] as String,
                    content = arguments["content"] as String
                )
                "file_list" -> fileList(
                    path = arguments["path"] as? String ?: "",
                    sort = arguments["sort"] as? String,
                    startLine = (arguments["startLine"] as? Double)?.toInt(),
                    count = (arguments["count"] as? Double)?.toInt()
                )
                else -> "Unknown tool: $toolName"
            }
        }
    }

    private val gson = Gson()

    private suspend fun runJs(js: String, timeoutMs: Long = 10000L): String = withTimeout(timeoutMs) {
        suspendCancellableCoroutine { continuation ->
            if (continuation.isCancelled) return@suspendCancellableCoroutine
            val tab = tabManager.getActiveTab()
            if (tab == null) {
                continuation.resume("No active tab")
                return@suspendCancellableCoroutine
            }
            tab.webView?.evaluateJavascript(js) { result ->
                val value = when {
                    result == null -> "Error: JS returned null"
                    result == "null" -> "Error: JS returned null value"
                    result.startsWith("\"") -> {
                        try { gson.fromJson(result, String::class.java) }
                        catch (_: Exception) { result.removeSurrounding("\"") }
                    }
                    else -> result
                }
                if (!continuation.isCompleted) continuation.resume(value)
            } ?: run {
                if (!continuation.isCompleted) continuation.resume("No WebView")
            }
        }
    }

    private fun parseFillFormFields(raw: Any?): List<Map<String, String>> {
        @Suppress("UNCHECKED_CAST")
        return when (raw) {
            is List<*> -> raw.map { item ->
                when (item) {
                    is Map<*, *> -> {
                        @Suppress("UNCHECKED_CAST")
                        item as Map<String, String>
                    }
                    is String -> {
                        val parts = item.split(",", limit = 2)
                        var target = ""
                        var value = ""
                        for (part in parts) {
                            val eq = part.indexOf('=')
                            if (eq > 0) {
                                val k = part.substring(0, eq).trim()
                                val v = part.substring(eq + 1).trim()
                                when (k) {
                                    "target" -> target = v
                                    "value" -> value = v
                                }
                            }
                        }
                        mapOf("target" to target, "value" to value)
                    }
                    else -> emptyMap()
                }
            }
            else -> emptyList()
        }
    }

    private fun resolveSelector(target: String): String {
        val cleaned = target
            .replace("[ref=", "")
            .replace("]", "")
            .removePrefix("ref=")
        return when {
            cleaned.matches(Regex("^(?:f\\d+)?e?\\d+$")) -> {
                val ref = if (cleaned.matches(Regex("\\d+"))) "e$cleaned" else cleaned
                "[data-ref='$ref']"
            }
            else -> target
        }
    }

    private fun isTextSelector(target: String): Boolean {
        return target.contains(":has-text(")
    }

    private fun parseTextSelector(target: String): Pair<String, String>? {
        val match = Regex("""(.+?):has-text\(["'](.+?)["']\)""").matchEntire(target)
        return match?.let { it.groupValues[1] to it.groupValues[2] }
    }

    private fun navigate(url: String): String {
        val tab = tabManager.getActiveTab() ?: return "No active tab"
        tab.webView?.loadUrl(url)
        return "Navigated to $url"
    }

    private fun navigateBack(): String {
        val tab = tabManager.getActiveTab() ?: return "No active tab"
        if (tab.webView?.canGoBack() == true) {
            tab.webView?.goBack()
            return "Went back"
        }
        return "Cannot go back"
    }

    private suspend fun snapshot(target: String?, depth: Int?, boxes: Boolean?): String {
        val maxDepth = depth ?: 20
        val targetSel = target?.let { resolveSelector(it) }
        val js = """
            (function() {
                var MAX_DEPTH = $maxDepth;
                var targetSel = ${if (targetSel != null) "\"" + targetSel + "\"" else "null"};
                var root = targetSel ? document.querySelector(targetSel) : (document.body || document.documentElement);
                if (targetSel && !root) return 'Target not found: $target';
                if (!root) return 'No page content';
                var refNum = 0;

                function getAccessibleRole(el) {
                    var tag = el.tagName.toLowerCase();
                    var role = el.getAttribute('role');
                    if (role) return role;
                    if (tag === 'a' && el.href) return 'link';
                    if (tag === 'button') return 'button';
                    if (tag === 'input') {
                        var t = (el.getAttribute('type') || 'text').toLowerCase();
                        if (t === 'checkbox') return 'checkbox';
                        if (t === 'radio') return 'radio';
                        if (t === 'submit' || t === 'reset') return 'button';
                        if (t === 'email' || t === 'url' || t === 'tel' || t === 'number') return 'textbox';
                        return 'textbox';
                    }
                    if (tag === 'textarea') return 'textbox';
                    if (tag === 'select') return 'combobox' + (el.multiple ? 'listbox' : '');
                    if (tag === 'img') return el.alt ? 'image' : 'presentation';
                    if (tag === 'h1' || tag === 'h2' || tag === 'h3' || tag === 'h4' || tag === 'h5' || tag === 'h6') return 'heading';
                    if (tag === 'nav') return 'navigation';
                    if (tag === 'main') return 'main';
                    if (tag === 'header') return 'banner';
                    if (tag === 'footer') return 'contentinfo';
                    if (tag === 'aside') return 'complementary';
                    if (tag === 'form') return 'form';
                    if (tag === 'table') return 'table';
                    if (tag === 'ul' || tag === 'ol') return 'list';
                    if (tag === 'li') return 'listitem';
                    if (tag.match(/^h[1-6]\$/)) return 'heading';
                    return tag;
                }

                function getAccessibleName(el) {
                    var aria = el.getAttribute('aria-label');
                    if (aria && aria.trim()) return aria.trim();
                    var labelledby = el.getAttribute('aria-labelledby');
                    if (labelledby) {
                        var ref = document.getElementById(labelledby);
                        if (ref && ref.textContent.trim()) return ref.textContent.trim().substring(0, 100);
                    }
                    if (el.alt && el.alt.trim()) return el.alt.trim();
                    if (el.placeholder && el.placeholder.trim()) return el.placeholder.trim();
                    if (el.title && el.title.trim()) return el.title.trim();
                    if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') return '';
                    var label = document.querySelector('label[for="' + el.id + '"]');
                    if (label && label.textContent.trim()) return label.textContent.trim().substring(0, 100);
                    if (el.textContent && el.textContent.trim()) {
                        var text = el.textContent.trim().substring(0, 80);
                        if (['a','button','h1','h2','h3','h4','h5','h6','li','option','th','td','summary'].indexOf(el.tagName.toLowerCase()) >= 0) return text;
                        if (el.getAttribute('role') === 'button' || el.getAttribute('role') === 'link' || el.getAttribute('role') === 'tab') return text;
                        return text.substring(0, 40);
                    }
                    return '';
                }

                function getProperties(el, role) {
                    var props = [];
                    if (el.disabled) props.push('disabled');
                    if (el.getAttribute('aria-disabled') === 'true') props.push('disabled');
                    if (el.getAttribute('aria-expanded') === 'true') props.push('expanded=true');
                    if (el.getAttribute('aria-expanded') === 'false') props.push('expanded=false');
                    var checked = el.getAttribute('aria-checked');
                    if (checked) props.push('checked=' + checked);
                    else if (role === 'checkbox' || role === 'radio') {
                        if (el.checked) props.push('checked=true');
                        else props.push('checked=false');
                    }
                    var pressed = el.getAttribute('aria-pressed');
                    if (pressed) props.push('pressed=' + pressed);
                    if (el.getAttribute('aria-selected') === 'true') props.push('selected=true');
                    if (el.getAttribute('aria-haspopup')) props.push('haspopup=' + el.getAttribute('aria-haspopup'));
                    if (el.getAttribute('aria-required') === 'true') props.push('required');
                    if (role === 'heading') {
                        var level = parseInt(el.tagName.charAt(1));
                        if (!isNaN(level)) props.push('level=' + level);
                    }
                    if (role === 'link' && el.href) props.push('url=' + el.href.substring(0, 200));
                    if (role === 'textbox' || role === 'combobox') {
                        var val = el.value || '';
                        if (val) props.push('value="' + val.replace(/"/g, '&quot;').substring(0, 100) + '"');
                    }
                    if (role === 'image' && el.src) props.push('src=' + el.src.substring(0, 200));
                    var desc = el.getAttribute('aria-description') || el.getAttribute('aria-describedby');
                    if (desc) props.push('description="' + desc.substring(0, 100) + '"');
                    return props;
                }

                function isInteractive(el) {
                    if (el.tagName.match(/^(A|BUTTON|INPUT|TEXTAREA|SELECT|SUMMARY|DETAILS)\$/i)) return true;
                    var role = el.getAttribute('role');
                    if (role && ['button','link','checkbox','radio','tab','menuitem','option','switch','combobox','slider'].indexOf(role) >= 0) return true;
                    var tabIndex = el.getAttribute('tabindex');
                    if (tabIndex !== null && parseInt(tabIndex) >= 0) return true;
                    if (el.onclick || el.getAttribute('ng-click') || el.getAttribute('@click')) return true;
                    return false;
                }

                var MAX_OUTPUT = 80000;
                var outputLen = 0;
                var truncated = false;

                function formatNode(child, currentDepth) {
                    if (currentDepth > MAX_DEPTH || truncated) return '';
                    if (child.offsetWidth === 0 && child.offsetHeight === 0) {
                        if (!isInteractive(child)) {
                            var hiddenResult = '';
                            for (var i = 0; i < child.children.length; i++) {
                                hiddenResult += formatNode(child.children[i], currentDepth);
                            }
                            return hiddenResult;
                        }
                    }
                    var role = getAccessibleRole(child);
                    var tag = child.tagName.toLowerCase();

                    var isSemantic = role !== tag || isInteractive(child);
                    if (!isSemantic) {
                        var wrapperResult = '';
                        for (var i = 0; i < child.children.length; i++) {
                            wrapperResult += formatNode(child.children[i], currentDepth);
                        }
                        return wrapperResult;
                    }

                    var name = getAccessibleName(child);
                    var props = getProperties(child, role);
                    refNum++;
                    var ref = 'e' + refNum;
                    child.setAttribute('data-ref', ref);

                    var line = '- ' + role;
                    if (name) line += ' "' + name.replace(/"/g, '&quot;') + '"';
                    var suffix = '';
                    if (props.length > 0) suffix += ' [' + props.join(', ') + ']';
                    ${if (boxes == true) """
                    var r = child.getBoundingClientRect();
                    suffix += ' [box=' + Math.round(r.x) + ',' + Math.round(r.y) + ',' + Math.round(r.width) + ',' + Math.round(r.height) + ']';
                    """ else ""}
                    if (suffix) line += suffix;
                    line += ' [ref=' + ref + ']\\n';

                    if (outputLen + line.length > MAX_OUTPUT) {
                        truncated = true;
                        return line;
                    }
                    outputLen += line.length;

                    for (var i = 0; i < child.children.length; i++) {
                        var childLine = formatNode(child.children[i], currentDepth + 1);
                        if (childLine && !truncated) {
                            var indented = childLine.split('\\n').filter(Boolean).map(function(l) { return '  ' + l; }).join('\\n') + '\\n';
                            line += indented;
                        }
                    }
                    return line;
                }

                function formatChildren(parent, currentDepth) {
                    var result = '';
                    for (var i = 0; i < parent.children.length; i++) {
                        result += formatNode(parent.children[i], currentDepth + 1);
                    }
                    return result;
                }

                var title = document.title || '';
                var url = window.location.href || '';
                var tree = '';
                for (var i = 0; i < root.children.length; i++) {
                    tree += formatNode(root.children[i], 0);
                }
                if (truncated) tree += '... (truncated at ' + MAX_OUTPUT + ' chars)\\n';
                return 'URL: ' + url + '\\nTitle: ' + title + '\\n\\n' + tree;
            })()
        """.trimIndent()
        return runJs(js, 30000L).replace("\\n", "\n")
    }

    private fun screenshot(fullPage: Boolean?): String {
        return "Screenshots require native Android WebView capture. Use browser_snapshot instead."
    }

    private suspend fun click(target: String, doubleClick: Boolean?, button: String?, modifiers: List<String>?, scrollIntoView: Boolean = true): String {
        val clickType = if (doubleClick == true) "dblclick" else "click"
        val btn = when (button) {
            "right" -> 2
            "middle" -> 1
            else -> 0
        }
        val ctrl = modifiers?.contains("Control") == true
        val shift = modifiers?.contains("Shift") == true
        val alt = modifiers?.contains("Alt") == true
        val meta = modifiers?.contains("Meta") == true
        val modProps = "ctrlKey: $ctrl, shiftKey: $shift, altKey: $alt, metaKey: $meta"
        val scrollJs = if (scrollIntoView) "el.scrollIntoView({behavior: 'instant', block: 'center'});" else ""

        if (isTextSelector(target)) {
            val parts = parseTextSelector(target)
            if (parts != null) {
                val (tag, text) = parts
                val escapedText = text.replace("'", "\\'")
                val js = """
                    (function() {
                        var els = Array.from(document.querySelectorAll('$tag'));
                        var el = els.find(function(e) { return e.textContent.trim().includes('$escapedText'); });
                        if (!el) return "Element not found: $target";
                        $scrollJs
                        el.dispatchEvent(new MouseEvent('$clickType', {
                            bubbles: true,
                            cancelable: true,
                            button: $btn,
                            $modProps
                        }));
                        el.focus();
                        return 'Clicked ' + el.tagName;
                    })()
                """.trimIndent()
                return runJs(js)
            }
        }

        val sel = resolveSelector(target)
        val js = """
            (function() {
                var el = document.querySelector("$sel");
                if (!el) return "Element not found: $target";
                $scrollJs
                el.dispatchEvent(new MouseEvent('$clickType', {
                    bubbles: true,
                    cancelable: true,
                    button: $btn,
                    $modProps
                }));
                el.focus();
                return 'Clicked ' + el.tagName;
            })()
        """.trimIndent()
        return runJs(js)
    }

    private suspend fun type(target: String, text: String, submit: Boolean?, slowly: Boolean?, scrollIntoView: Boolean = true): String {
        val escaped = text.replace("'", "\\'").replace("\n", "\\n")
        val scrollJs = if (scrollIntoView) "el.scrollIntoView({behavior: 'instant', block: 'center'});" else ""

        val findElJs = if (isTextSelector(target)) {
            val parts = parseTextSelector(target)
            if (parts != null) {
                val (tag, textSel) = parts
                val escText = textSel.replace("'", "\\'")
                "var els = Array.from(document.querySelectorAll('$tag')); var el = els.find(function(e) { return e.textContent.trim().includes('$escText'); });"
            } else {
                val sel = resolveSelector(target)
                "var el = document.querySelector('$sel');"
            }
        } else {
            val sel = resolveSelector(target)
            "var el = document.querySelector('$sel');"
        }

        if (slowly == true) {
            val js = """
                (function() {
                    $findElJs
                    if (!el) return 'Element not found: $target';
                    $scrollJs
                    el.focus();
                    el.value = '';
                    for (var i = 0; i < '$escaped'.length; i++) {
                        el.value += '$escaped'[i];
                        el.dispatchEvent(new Event('input', { bubbles: true }));
                    }
                    el.dispatchEvent(new Event('change', { bubbles: true }));
                    ${if (submit == true) "el.dispatchEvent(new KeyboardEvent('keydown', {key:'Enter', keyCode:13, bubbles:true}));" else ""}
                    return 'Typed into ' + el.tagName;
                })()
            """.trimIndent()
            return runJs(js)
        }
        // fill (default)
        val js = """
            (function() {
                $findElJs
                if (!el) return "Element not found: $target";
                $scrollJs
                el.focus();
                el.value = '$escaped';
                el.dispatchEvent(new Event('input', { bubbles: true }));
                el.dispatchEvent(new Event('change', { bubbles: true }));
                ${if (submit == true) "el.dispatchEvent(new KeyboardEvent('keydown', {key:'Enter', keyCode:13, bubbles:true}));" else ""}
                return 'Typed into ' + el.tagName;
            })()
        """.trimIndent()
        return runJs(js)
    }

    private suspend fun fillForm(fields: List<Map<String, String>>, scrollIntoView: Boolean = true): String {
        val scrollJs = if (scrollIntoView) "el.scrollIntoView({behavior: 'instant', block: 'center'});" else ""
        val results = mutableListOf<String>()
        for (field in fields) {
            val selector = field["selector"] ?: continue
            val value = field["value"] ?: continue
            val sel = resolveSelector(selector)
            val escaped = value.replace("'", "\\'").replace("\n", "\\n")
            val js = """
                (function() {
                    var el = document.querySelector('$sel');
                    if (!el) return "Not found: $selector";
                    $scrollJs
                    el.focus();
                    el.value = '$escaped';
                    el.dispatchEvent(new Event('input', { bubbles: true }));
                    return 'Filled ' + el.tagName;
                })()
            """.trimIndent()
            results.add(runJs(js))
        }
        return results.joinToString("; ")
    }

    private suspend fun selectOption(target: String, values: List<String>, scrollIntoView: Boolean = true): String {
        val sel = resolveSelector(target)
        val scrollJs = if (scrollIntoView) "el.scrollIntoView({behavior: 'instant', block: 'center'});" else ""
        val valArr = values.joinToString(",") { "\"$it\"" }
        val js = """
            (function() {
                var el = document.querySelector("$sel");
                if (!el) return "Element not found: $target";
                $scrollJs
                var vals = [$valArr];
                for (var i = 0; i < el.options.length; i++) {
                    if (vals.includes(el.options[i].value)) {
                        el.options[i].selected = true;
                    }
                }
                el.dispatchEvent(new Event('change', { bubbles: true }));
                return 'Selected ' + vals.length + ' option(s)';
            })()
        """.trimIndent()
        return runJs(js)
    }

    private suspend fun hover(target: String, scrollIntoView: Boolean = true): String {
        val sel = resolveSelector(target)
        val scrollJs = if (scrollIntoView) "el.scrollIntoView({behavior: 'instant', block: 'center'});" else ""
        val js = """
            (function() {
                var el = document.querySelector("$sel");
                if (!el) return "Element not found: $target";
                $scrollJs
                el.dispatchEvent(new MouseEvent('mouseenter', { bubbles: true }));
                el.dispatchEvent(new MouseEvent('mouseover', { bubbles: true }));
                return 'Hovered ' + el.tagName;
            })()
        """.trimIndent()
        return runJs(js)
    }

    private suspend fun pressKey(key: String): String {
        val keyCode = when (key.lowercase()) {
            "enter" -> 13
            "tab" -> 9
            "escape" -> 27
            "backspace" -> 8
            "delete" -> 46
            "arrowleft" -> 37
            "arrowup" -> 38
            "arrowright" -> 39
            "arrowdown" -> 40
            else -> key.firstOrNull()?.code ?: 0
        }
        val js = """
            (function() {
                var el = document.activeElement || document.body;
                el.dispatchEvent(new KeyboardEvent('keydown', {key:'$key', keyCode:$keyCode, bubbles:true}));
                el.dispatchEvent(new KeyboardEvent('keyup', {key:'$key', keyCode:$keyCode, bubbles:true}));
                return 'Pressed $key';
            })()
        """.trimIndent()
        return runJs(js)
    }

    private suspend fun evaluate(function: String, target: String?, scrollIntoView: Boolean = true): String {
        if (target != null) {
            val sel = resolveSelector(target)
            val scrollJs = if (scrollIntoView) "el.scrollIntoView({behavior: 'instant', block: 'center'});" else ""
            return runJs("""(function() { var el = document.querySelector("$sel"); if (!el) return "Element not found"; $scrollJs return (function(){ $function })(); })()""")
        }
        return runJs("""($function)()""")
    }

    private suspend fun waitFor(text: String?, textGone: String?, time: Double?): String {
        if (time != null) {
            delay((time * 1000).toLong())
            return "Waited ${time}s"
        }
        if (textGone != null) {
            val js = """
                (function() {
                    var pageText = document.body.innerText;
                    return pageText.includes('$textGone') ? 'Still visible: $textGone' : 'Text gone: $textGone';
                })()
            """.trimIndent()
            return runJs(js)
        }
        val js = """
            (function() {
                var pageText = document.body.innerText;
                return pageText.includes('${text ?: ""}') ? 'Found: ${text ?: ""}' : 'Not found: ${text ?: ""}';
            })()
        """.trimIndent()
        return runJs(js)
    }

    private suspend fun drag(startTarget: String, endTarget: String, scrollIntoView: Boolean = true): String {
        val startSel = resolveSelector(startTarget)
        val endSel = resolveSelector(endTarget)
        val scrollJs = if (scrollIntoView) """
                startEl.scrollIntoView({behavior: 'instant', block: 'center'});
                endEl.scrollIntoView({behavior: 'instant', block: 'center'});
        """.trimIndent() else ""
        val js = """
            (function() {
                var startEl = document.querySelector("$startSel");
                var endEl = document.querySelector("$endSel");
                if (!startEl) return "Start element not found: $startTarget";
                if (!endEl) return "End element not found: $endTarget";
                $scrollJs
                var startBox = startEl.getBoundingClientRect();
                var endBox = endEl.getBoundingClientRect();
                var sx = startBox.left + startBox.width / 2;
                var sy = startBox.top + startBox.height / 2;
                var ex = endBox.left + endBox.width / 2;
                var ey = endBox.top + endBox.height / 2;
                startEl.dispatchEvent(new MouseEvent('mousedown', {bubbles: true, clientX: sx, clientY: sy}));
                endEl.dispatchEvent(new MouseEvent('mousemove', {bubbles: true, clientX: ex, clientY: ey}));
                endEl.dispatchEvent(new MouseEvent('mouseup', {bubbles: true, clientX: ex, clientY: ey}));
                return 'Dragged to ' + endEl.tagName;
            })()
        """.trimIndent()
        return runJs(js)
    }

    private suspend fun handleDialog(accept: Boolean, promptText: String?): String {
        return "Dialog ${if (accept) "accepted" else "dismissed"}" + if (promptText != null) " with: $promptText" else ""
    }

    private suspend fun consoleMessages(level: String?): String {
        return "Console messages: (requires WebView ChromeClient integration)"
    }

    private fun resize(width: Int, height: Int): String {
        return "Resize requires native API - use browser settings"
    }

    private fun tabs(action: String, index: Int?, url: String?): String {
        return when (action) {
            "list" -> {
                val tabs = tabManager.tabs
                tabs.mapIndexed { i, t -> "$i: ${t.title} (${t.url})" }.joinToString("\n")
            }
            "new" -> {
                tabManager.createTab(url ?: "about:blank")
                "Created new tab"
            }
            "close" -> {
                val tabs = tabManager.tabs
                val idx = index ?: tabs.size - 1
                if (idx in tabs.indices) {
                    tabManager.closeTab(tabs[idx].id)
                    "Closed tab $idx"
                } else "Invalid index: $idx"
            }
            "select" -> {
                val tabs = tabManager.tabs
                if (index != null && index in tabs.indices) {
                    tabManager.setActiveTab(tabs[index].id)
                    "Selected tab $index: ${tabs[index].title}"
                } else "Invalid index: $index"
            }
            else -> "Unknown tab action: $action"
        }
    }

    private suspend fun resolveRoot(): DocumentFile? {
        val uriStr = settingsRepository.notesDirectoryUri.first()
        if (uriStr.isNullOrBlank()) return null
        val uri = Uri.parse(uriStr)
        return DocumentFile.fromTreeUri(context, uri)
    }

    private fun findDocument(parent: DocumentFile, path: String): DocumentFile? {
        val parts = path.trim('/').split("/").filter { it.isNotEmpty() }
        if (parts.isEmpty()) return parent
        var current = parent
        for ((i, name) in parts.withIndex()) {
            val child = current.findFile(name) ?: return null
            if (i == parts.size - 1) return child
            if (child.isDirectory) current = child
            else return null
        }
        return current
    }

    private fun resolveOrCreateFile(root: DocumentFile, path: String): DocumentFile? {
        val parts = path.trim('/').split("/").filter { it.isNotEmpty() }
        if (parts.isEmpty()) return null
        var current = root
        for ((i, name) in parts.withIndex()) {
            val isLast = i == parts.size - 1
            var child = current.findFile(name)
            if (child == null) {
                child = if (isLast) current.createFile("application/octet-stream", name)
                         else current.createDirectory(name)
            }
            if (child == null) return null
            if (isLast) return child
            if (!child.isDirectory) return null
            current = child
        }
        return null
    }

    private suspend fun fileRead(path: String, offset: Int, length: Int): String = withContext(Dispatchers.IO) {
        val root = resolveRoot() ?: return@withContext "Notes directory not configured. Go to Settings → Behavior to set one."
        val file = findDocument(root, path)
            ?: return@withContext "File not found: $path"
        if (file.isDirectory) return@withContext "Cannot read a directory: $path"

        val start = if (offset >= 0) offset else 0
        val maxLen = if (length > 0) length else Int.MAX_VALUE

        context.contentResolver.openInputStream(file.uri)?.use { input ->
            if (start > 0) {
                var skipped = 0L
                while (skipped < start) {
                    val s = input.skip(start - skipped)
                    if (s <= 0) break
                    skipped += s
                }
            }
            val reader = BufferedReader(InputStreamReader(input))
            val buf = CharArray(minOf(maxLen.coerceAtMost(1048576), 65536))
            val sb = StringBuilder()
            var totalRead = 0
            while (totalRead < maxLen) {
                val toRead = minOf(buf.size, maxLen - totalRead)
                val n = reader.read(buf, 0, toRead)
                if (n < 0) break
                sb.append(buf, 0, n)
                totalRead += n
            }
            if (sb.length >= maxLen) sb.append("\n... (truncated at $maxLen bytes)")
            return@withContext sb.toString()
        } ?: return@withContext "Failed to open file: $path"
    }

    private suspend fun fileWrite(path: String, content: String): String = withContext(Dispatchers.IO) {
        val root = resolveRoot() ?: return@withContext "Notes directory not configured. Go to Settings → Behavior to set one."

        val existing = findDocument(root, path)
        if (existing != null && existing.isDirectory) {
            return@withContext "Cannot overwrite a directory: $path"
        }

        if (existing != null) {
            if (!existing.delete()) return@withContext "Failed to delete existing file: $path"
        }

        val file = resolveOrCreateFile(root, path)
            ?: return@withContext "Failed to create file: $path"

        val bytes = content.toByteArray(Charsets.UTF_8)
        context.contentResolver.openOutputStream(file.uri, "wt")?.use { out ->
            out.write(bytes)
            out.flush()
        } ?: return@withContext "Failed to open file for writing: $path"

        "Written ${bytes.size} bytes to $path"
    }

    private suspend fun fileList(path: String, sort: String?, startLine: Int?, count: Int?): String = withContext(Dispatchers.IO) {
        val root = resolveRoot() ?: return@withContext "Notes directory not configured. Go to Settings → Behavior to set one."

        val dir = if (path.isBlank()) root else findDocument(root, path)
            ?: return@withContext "Directory not found: $path"
        if (!dir.isDirectory) return@withContext "Not a directory: $path"

        val allFiles = dir.listFiles().toList()
        val sorted = when (sort?.lowercase()) {
            "size" -> allFiles.sortedBy { it.length() }
            "date" -> allFiles.sortedByDescending { it.lastModified() }
            "created_date" -> allFiles.sortedByDescending { it.lastModified() }
            else -> allFiles.sortedBy { it.name?.lowercase() ?: "" }
        }

        val start = (startLine ?: 0).coerceIn(0, sorted.size)
        val end = if (count != null) (start + count).coerceAtMost(sorted.size) else sorted.size
        val page = sorted.subList(start, end)
        val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

        val sb = StringBuilder()
        sb.appendLine("Directory: ${path.ifBlank { "/" }}")
        for (f in page) {
            val name = f.name ?: "unknown"
            val displayName = if (f.isDirectory) "$name/" else name
            val sizeStr = if (f.isDirectory) "(dir)" else formatSize(f.length())
            val modified = dateFmt.format(Date(f.lastModified()))
            val line = "  ${displayName.padEnd(36)} ${sizeStr.padEnd(10)} $modified"
            sb.appendLine(line)
        }
        val total = sorted.size
        if (total == 0) sb.appendLine("(empty)")
        sb.appendLine("$total entries" + if (end < total) " (showing ${start+1}-$end of $total)" else "")
        return@withContext sb.toString().trimEnd()
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
            else -> "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
        }
    }
}
