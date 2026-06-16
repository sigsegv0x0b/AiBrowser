package com.aibrowser.agent

import com.aibrowser.browser.TabManager
import com.aibrowser.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolExecutor @Inject constructor(
    private val tabManager: TabManager,
    private val settingsRepository: SettingsRepository
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
                else -> "Unknown tool: $toolName"
            }
        }
    }

    private suspend fun runJs(js: String): String = withTimeout(10000L) {
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
                    else -> result.removeSurrounding("\"")
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
                            return formatChildren(child, currentDepth);
                        }
                    }
                    var role = getAccessibleRole(child);
                    var tag = child.tagName.toLowerCase();

                    var isSemantic = role !== tag || isInteractive(child);
                    if (!isSemantic) {
                        return formatChildren(child, currentDepth);
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
        return runJs(js).replace("\\n", "\n")
    }

    private fun screenshot(fullPage: Boolean?): String {
        return "Screenshots require native Android WebView capture. Use browser_snapshot instead."
    }

    private suspend fun click(target: String, doubleClick: Boolean?, button: String?, modifiers: List<String>?, scrollIntoView: Boolean = true): String {
        val sel = resolveSelector(target)
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
        val sel = resolveSelector(target)
        val escaped = text.replace("'", "\\'").replace("\n", "\\n")
        val scrollJs = if (scrollIntoView) "el.scrollIntoView({behavior: 'instant', block: 'center'});" else ""
        if (slowly == true) {
            val js = """
                (function() {
                    var el = document.querySelector('$sel');
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
                var el = document.querySelector("$sel");
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
}
