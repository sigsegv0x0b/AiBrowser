package com.aibrowser.agent

import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet

@Suppress("UNUSED")
class BrowserToolSet : ToolSet {

    @Tool(description = "Navigate to a URL")
    fun browser_navigate(
        @ToolParam(description = "The URL to navigate to") url: String
    ): Map<String, String> = empty()

    @Tool(description = "Go back to the previous page in history")
    fun browser_navigate_back(): Map<String, String> = empty()

    @Tool(description = "Capture accessibility snapshot of the current page. Returns page URL, title, and accessibility tree with interactive element references (e.g. [ref=e42]). Use this first before any page interaction.")
    fun browser_snapshot(
        @ToolParam(description = "Element ref or selector to snapshot instead of full page") target: String? = null,
        @ToolParam(description = "Limit the depth of the snapshot tree") depth: Int? = null,
        @ToolParam(description = "Include each element's bounding box as [box=x,y,width,height]") boxes: Boolean? = null
    ): Map<String, String> = empty()

    @Tool(description = "Takes a screenshot of the current page. Prefer browser_snapshot for understanding page content.")
    fun browser_take_screenshot(
        @ToolParam(description = "Capture full scrollable page") fullPage: Boolean? = null,
        @ToolParam(description = "Image format") type: String? = null
    ): Map<String, String> = empty()

    @Tool(description = "Click an element on the page")
    fun browser_click(
        @ToolParam(description = "Human-readable description of the element for logging") element: String? = null,
        @ToolParam(description = "Element ref from snapshot (e.g. e42) or CSS selector") target: String,
        @ToolParam(description = "Perform double click") doubleClick: Boolean? = null,
        @ToolParam(description = "Mouse button") button: String? = null,
        @ToolParam(description = "Keyboard modifiers") modifiers: List<String>? = null
    ): Map<String, String> = empty()

    @Tool(description = "Type text into an editable element. Uses fill by default; use slowly for character-by-character typing.")
    fun browser_type(
        @ToolParam(description = "Human-readable description of the element") element: String? = null,
        @ToolParam(description = "Element ref from snapshot (e.g. e42) or CSS selector") target: String,
        @ToolParam(description = "Text to type into the element") text: String,
        @ToolParam(description = "Press Enter after typing to submit the form") submit: Boolean? = null,
        @ToolParam(description = "Type character by character instead of using fill") slowly: Boolean? = null
    ): Map<String, String> = empty()

    @Tool(description = "Fill multiple form fields at once")
    fun browser_fill_form(
        @ToolParam(description = "List of field descriptor strings in format 'target=CssSelector,value=text'") fields: List<String>
    ): Map<String, String> = empty()

    @Tool(description = "Select option(s) in a dropdown")
    fun browser_select_option(
        @ToolParam(description = "Human-readable description of the element") element: String? = null,
        @ToolParam(description = "Element ref from snapshot (e.g. e42) or CSS selector") target: String,
        @ToolParam(description = "Values to select") values: List<String>
    ): Map<String, String> = empty()

    @Tool(description = "Hover over an element")
    fun browser_hover(
        @ToolParam(description = "Human-readable description of the element") element: String? = null,
        @ToolParam(description = "Element ref from snapshot (e.g. e42) or CSS selector") target: String
    ): Map<String, String> = empty()

    @Tool(description = "Press a keyboard key")
    fun browser_press_key(
        @ToolParam(description = "Key name: Enter, Tab, Escape, ArrowDown, etc.") key: String
    ): Map<String, String> = empty()

    @Tool(description = "Evaluate JavaScript on the page")
    fun browser_evaluate(
        @ToolParam(description = "JS function: () => { return document.title; }") function: String,
        @ToolParam(description = "Element ref or selector to evaluate in context") target: String? = null
    ): Map<String, String> = empty()

    @Tool(description = "Wait for text to appear, disappear, or for a time period")
    fun browser_wait_for(
        @ToolParam(description = "Text to wait for on the page") text: String? = null,
        @ToolParam(description = "Text to wait to disappear") textGone: String? = null,
        @ToolParam(description = "Seconds to wait") time: Double? = null
    ): Map<String, String> = empty()

    @Tool(description = "List, create, close, or select browser tabs")
    fun browser_tabs(
        @ToolParam(description = "Tab operation") action: String,
        @ToolParam(description = "Tab index for close/select") index: Int? = null,
        @ToolParam(description = "URL for new tab") url: String? = null
    ): Map<String, String> = empty()

    @Tool(description = "Drag an element to another element")
    fun browser_drag(
        @ToolParam(description = "Human-readable description of the start element") startElement: String? = null,
        @ToolParam(description = "Element ref or selector for the element to drag") startTarget: String,
        @ToolParam(description = "Human-readable description of the target element") endElement: String? = null,
        @ToolParam(description = "Element ref or selector for the drop target") endTarget: String
    ): Map<String, String> = empty()

    @Tool(description = "Accept or dismiss a browser dialog (alert, confirm, prompt)")
    fun browser_handle_dialog(
        @ToolParam(description = "Accept (true) or dismiss (false) the dialog") accept: Boolean,
        @ToolParam(description = "Text to enter for prompt dialogs") promptText: String? = null
    ): Map<String, String> = empty()

    @Tool(description = "Retrieve console messages from the page")
    fun browser_console_messages(
        @ToolParam(description = "Filter by console level") level: String? = null
    ): Map<String, String> = empty()

    @Tool(description = "Resize the browser viewport")
    fun browser_resize(
        @ToolParam(description = "New viewport width") width: Int,
        @ToolParam(description = "New viewport height") height: Int
    ): Map<String, String> = empty()

    @Tool(description = "Read a file from the notes directory")
    fun file_read(
        @ToolParam(description = "Relative path to the file within the notes directory") path: String,
        @ToolParam(description = "Byte offset to start reading from (0-based, default 0)") offset: Int? = null,
        @ToolParam(description = "Maximum number of bytes to read (default: entire file)") length: Int? = null
    ): Map<String, String> = empty()

    @Tool(description = "Write content to a file in the notes directory. Creates parent directories if needed.")
    fun file_write(
        @ToolParam(description = "Relative path to the file within the notes directory") path: String,
        @ToolParam(description = "Content to write to the file") content: String
    ): Map<String, String> = empty()

    @Tool(description = "List files and directories within the notes directory")
    fun file_list(
        @ToolParam(description = "Relative path within the notes directory (default: root)") path: String? = null,
        @ToolParam(description = "Sort mode: name, size, date, or created_date") sort: String? = null,
        @ToolParam(description = "Starting index for pagination (0-based)") startLine: Int? = null,
        @ToolParam(description = "Maximum number of entries to return") count: Int? = null
    ): Map<String, String> = empty()

    private fun empty(): Map<String, String> = emptyMap()
}
