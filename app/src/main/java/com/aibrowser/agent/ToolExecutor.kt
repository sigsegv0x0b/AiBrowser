package com.aibrowser.agent

import com.aibrowser.browser.BrowserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolExecutor @Inject constructor(
    private val browserViewModel: BrowserViewModel
) {
    suspend fun execute(toolName: String, arguments: Map<String, Any>): String {
        return withContext(Dispatchers.Main) {
            when (toolName) {
                "browser_navigate" -> navigate(arguments["url"] as String)
                "browser_navigate_back" -> navigateBack()
                "browser_snapshot" -> snapshot(
                    depth = (arguments["depth"] as? Double)?.toInt(),
                    boxes = arguments["boxes"] as? Boolean
                )
                "browser_take_screenshot" -> screenshot(
                    fullPage = arguments["fullPage"] as? Boolean
                )
                "browser_click" -> click(
                    target = arguments["target"] as String,
                    doubleClick = arguments["doubleClick"] as? Boolean,
                    button = arguments["button"] as? String
                )
                "browser_type" -> type(
                    target = arguments["target"] as String,
                    text = arguments["text"] as String,
                    submit = arguments["submit"] as? Boolean
                )
                "browser_fill_form" -> fillForm(arguments["fields"] as List<Map<String, String>>)
                "browser_select_option" -> selectOption(
                    target = arguments["target"] as String,
                    values = arguments["values"] as List<String>
                )
                "browser_hover" -> hover(arguments["target"] as String)
                "browser_press_key" -> pressKey(arguments["key"] as String)
                "browser_evaluate" -> evaluate(arguments["function"] as String)
                "browser_wait_for" -> waitFor(
                    text = arguments["text"] as? String,
                    time = arguments["time"] as? Double
                )
                "browser_tabs" -> tabs(
                    action = arguments["action"] as String,
                    index = (arguments["index"] as? Double)?.toInt(),
                    url = arguments["url"] as? String
                )
                else -> "Unknown tool: $toolName"
            }
        }
    }

    private fun runJs(js: String): String {
        var result = ""
        browserViewModel.getActiveTab()?.webView?.evaluateJavascript(js) {
            result = it?.removeSurrounding("\"") ?: "Error"
        }
        return result
    }

    private fun navigate(url: String): String {
        val tab = browserViewModel.getActiveTab() ?: return "No active tab"
        tab.webView?.loadUrl(url)
        return "Navigated to $url"
    }

    private fun navigateBack(): String {
        val tab = browserViewModel.getActiveTab() ?: return "No active tab"
        if (tab.webView?.canGoBack() == true) {
            tab.webView?.goBack()
            return "Went back"
        }
        return "Cannot go back"
    }

    private fun snapshot(depth: Int?, boxes: Boolean?): String {
        val js = """
            (function() {
                function getRole(el) {
                    var tag = el.tagName.toLowerCase();
                    var role = el.getAttribute('role') || '';
                    if (tag === 'a') return 'link';
                    if (tag === 'button' || role === 'button') return 'button';
                    if (tag === 'input' || tag === 'textarea' || tag === 'select') return 'textbox';
                    if (tag === 'img') return 'image';
                    if (tag === 'h1' || tag === 'h2' || tag === 'h3') return 'heading';
                    return role || tag;
                }
                function getLabel(el) {
                    if (el.id) return '#' + el.id;
                    if (el.name) return '[name="' + el.name + '"]';
                    if (el.placeholder) return el.placeholder;
                    if (el.textContent.trim()) return el.textContent.trim().substring(0, 50);
                    return el.tagName.toLowerCase();
                }
                function walk(el, indent) {
                    var lines = '';
                    var children = el.children;
                    for (var i = 0; i < children.length; i++) {
                        var child = children[i];
                        if (child.offsetWidth === 0 && child.offsetHeight === 0) continue;
                        var role = getRole(child);
                        var label = getLabel(child);
                        var ref = 'ref' + i + '_' + Math.random().toString(36).substr(2, 4);
                        child.setAttribute('data-ref', ref);
                        var box = '';
                        ${if (boxes == true) """var r = child.getBoundingClientRect();
                        box = ' [' + Math.round(r.x) + ',' + Math.round(r.y) + ',' + Math.round(r.width) + ',' + Math.round(r.height) + ']';""" else ""}
                        lines += ' '.repeat(indent) + '[' + ref + ' ' + role + '] ' + label + box + '\n';
                        lines += walk(child, indent + 2);
                    }
                    return lines;
                }
                return walk(document.body, 0);
            })()
        """.trimIndent()
        return runJs(js).replace("\\n", "\n")
    }

    private fun screenshot(fullPage: Boolean?): String {
        return "Screenshots require native Android WebView capture. Use browser_snapshot instead."
    }

    private fun click(target: String, doubleClick: Boolean?, button: String?): String {
        val clickType = if (doubleClick == true) "dblclick" else "click"
        val js = """
            (function() {
                var el = document.querySelector('$target');
                if (!el) return 'Element not found: $target';
                el.dispatchEvent(new MouseEvent('$clickType', {bubbles:true, cancelable:true, button:'${button ?: "left"}'}));
                return 'Clicked ' + el.tagName;
            })()
        """.trimIndent()
        return runJs(js)
    }

    private fun type(target: String, text: String, submit: Boolean?): String {
        val escaped = text.replace("'", "\\'").replace("\n", "\\n")
        val js = """
            (function() {
                var el = document.querySelector('$target');
                if (!el) return 'Element not found: $target';
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

    private fun fillForm(fields: List<Map<String, String>>): String {
        val results = mutableListOf<String>()
        for (field in fields) {
            val selector = field["selector"] ?: continue
            val value = field["value"] ?: continue
            val escaped = value.replace("'", "\\'").replace("\n", "\\n")
            val js = """
                (function() {
                    var el = document.querySelector('$selector');
                    if (!el) return 'Not found: $selector';
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

    private fun selectOption(target: String, values: List<String>): String {
        val js = """
            (function() {
                var el = document.querySelector('$target');
                if (!el) return 'Element not found: $target';
                var vals = ${values.map { "\"$it\"" }.toString()};
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

    private fun hover(target: String): String {
        val js = """
            (function() {
                var el = document.querySelector('$target');
                if (!el) return 'Element not found: $target';
                el.dispatchEvent(new MouseEvent('mouseenter', { bubbles: true }));
                el.dispatchEvent(new MouseEvent('mouseover', { bubbles: true }));
                return 'Hovered ' + el.tagName;
            })()
        """.trimIndent()
        return runJs(js)
    }

    private fun pressKey(key: String): String {
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

    private fun evaluate(function: String): String {
        var result = ""
        browserViewModel.getActiveTab()?.webView?.evaluateJavascript("($function)()") {
            result = it?.removeSurrounding("\"") ?: "undefined"
        }
        return result
    }

    private fun waitFor(text: String?, time: Double?): String {
        if (time != null) {
            return "Waited ${time}s"
        }
        val js = """
            (function() {
                var text = document.body.innerText;
                var found = text.includes('${text ?: ""}');
                return found ? 'Found: ${text ?: ""}' : 'Not found: ${text ?: ""}';
            })()
        """.trimIndent()
        return runJs(js)
    }

    private fun tabs(action: String, index: Int?, url: String?): String {
        return when (action) {
            "list" -> {
                val tabs = browserViewModel.tabs.value
                tabs.mapIndexed { i, t -> "$i: ${t.title} (${t.url})" }.joinToString("\n")
            }
            "new" -> {
                browserViewModel.createTab(url ?: "about:blank")
                "Created new tab"
            }
            "close" -> {
                val tabs = browserViewModel.tabs.value
                val idx = index ?: tabs.size - 1
                if (idx in tabs.indices) {
                    browserViewModel.closeTab(tabs[idx].id)
                    "Closed tab $idx"
                } else "Invalid index: $idx"
            }
            "select" -> {
                val tabs = browserViewModel.tabs.value
                if (index != null && index in tabs.indices) {
                    browserViewModel.setActiveTab(tabs[index].id)
                    "Selected tab $index: ${tabs[index].title}"
                } else "Invalid index: $index"
            }
            else -> "Unknown tab action: $action"
        }
    }
}
