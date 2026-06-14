package com.aibrowser.agent

import com.google.gson.Gson
import com.google.gson.JsonObject

object ToolDefinitions {
    private val gson = Gson()

    data class Tool(
        val name: String,
        val description: String,
        val parameters: JsonObject
    )

    val tools: List<Tool> = listOf(
        Tool(
            name = "browser_navigate",
            description = "Navigate to a URL",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "url": {"type": "string", "description": "The URL to navigate to"}
                },
                "required": ["url"]
            }""", JsonObject::class.java)
        ),
        Tool(
            name = "browser_navigate_back",
            description = "Go back to the previous page in history",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {}
            }""", JsonObject::class.java)
        ),
        Tool(
            name = "browser_snapshot",
            description = "Capture accessibility snapshot of the current page, this is better than screenshot. Returns the page URL, title, and accessibility tree with interactive element references (e.g. [ref=e42]). Use this first before any page interaction.",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "target": {"type": "string", "description": "Element ref or selector to snapshot instead of full page"},
                    "depth": {"type": "integer", "description": "Limit the depth of the snapshot tree"},
                    "boxes": {"type": "boolean", "description": "Include each element's bounding box as [box=x,y,width,height] in the snapshot"}
                }
            }""", JsonObject::class.java)
        ),
        Tool(
            name = "browser_take_screenshot",
            description = "Takes a screenshot of the current page. Prefer browser_snapshot for understanding page content.",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "fullPage": {"type": "boolean", "description": "Capture full scrollable page"},
                    "type": {"type": "string", "enum": ["png", "jpeg"], "description": "Image format"}
                }
            }""", JsonObject::class.java)
        ),
        Tool(
            name = "browser_click",
            description = "Click an element on the page",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "element": {"type": "string", "description": "Human-readable description of the element (for logging)"},
                    "target": {"type": "string", "description": "Element ref from snapshot (e.g. e42) or CSS selector"},
                    "doubleClick": {"type": "boolean", "description": "Perform double click"},
                    "button": {"type": "string", "enum": ["left", "right", "middle"], "description": "Mouse button"},
                    "modifiers": {"type": "array", "items": {"type": "string", "enum": ["Alt", "Control", "ControlOrMeta", "Meta", "Shift"]}, "description": "Keyboard modifiers"}
                },
                "required": ["target"]
            }""", JsonObject::class.java)
        ),
        Tool(
            name = "browser_type",
            description = "Type text into an editable element. Uses fill by default; use slowly for character-by-character typing.",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "element": {"type": "string", "description": "Human-readable description of the element"},
                    "target": {"type": "string", "description": "Element ref from snapshot (e.g. e42) or CSS selector"},
                    "text": {"type": "string", "description": "Text to type into the element"},
                    "submit": {"type": "boolean", "description": "Press Enter after typing to submit the form"},
                    "slowly": {"type": "boolean", "description": "Type character by character instead of using fill"}
                },
                "required": ["target", "text"]
            }""", JsonObject::class.java)
        ),
        Tool(
            name = "browser_fill_form",
            description = "Fill multiple form fields at once",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "fields": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "target": {"type": "string", "description": "Element ref or selector"},
                                "value": {"type": "string", "description": "Value to fill"}
                            }
                        }
                    }
                },
                "required": ["fields"]
            }""", JsonObject::class.java)
        ),
        Tool(
            name = "browser_select_option",
            description = "Select option(s) in a dropdown",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "element": {"type": "string", "description": "Human-readable description of the element"},
                    "target": {"type": "string", "description": "Element ref from snapshot (e.g. e42) or CSS selector"},
                    "values": {"type": "array", "items": {"type": "string"}, "description": "Values to select"}
                },
                "required": ["target", "values"]
            }""", JsonObject::class.java)
        ),
        Tool(
            name = "browser_hover",
            description = "Hover over an element",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "element": {"type": "string", "description": "Human-readable description of the element"},
                    "target": {"type": "string", "description": "Element ref from snapshot (e.g. e42) or CSS selector"}
                },
                "required": ["target"]
            }""", JsonObject::class.java)
        ),
        Tool(
            name = "browser_press_key",
            description = "Press a keyboard key",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "key": {"type": "string", "description": "Key name: Enter, Tab, Escape, ArrowDown, etc."}
                },
                "required": ["key"]
            }""", JsonObject::class.java)
        ),
        Tool(
            name = "browser_evaluate",
            description = "Evaluate JavaScript on the page",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "function": {"type": "string", "description": "JS function: () => { return document.title; }"},
                    "target": {"type": "string", "description": "Element ref or selector to evaluate in context of that element"}
                },
                "required": ["function"]
            }""", JsonObject::class.java)
        ),
        Tool(
            name = "browser_wait_for",
            description = "Wait for text to appear, disappear, or for a time period",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "text": {"type": "string", "description": "Text to wait for on the page"},
                    "textGone": {"type": "string", "description": "Text to wait to disappear"},
                    "time": {"type": "number", "description": "Seconds to wait"}
                }
            }""", JsonObject::class.java)
        ),
        Tool(
            name = "browser_tabs",
            description = "List, create, close, or select browser tabs",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "action": {"type": "string", "enum": ["list", "new", "close", "select"], "description": "Tab operation"},
                    "index": {"type": "integer", "description": "Tab index for close/select"},
                    "url": {"type": "string", "description": "URL for new tab"}
                },
                "required": ["action"]
            }""", JsonObject::class.java)
        ),
        Tool(
            name = "browser_drag",
            description = "Drag an element to another element",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "startElement": {"type": "string", "description": "Human-readable description of the start element"},
                    "startTarget": {"type": "string", "description": "Element ref or selector for the element to drag"},
                    "endElement": {"type": "string", "description": "Human-readable description of the target element"},
                    "endTarget": {"type": "string", "description": "Element ref or selector for the drop target"}
                },
                "required": ["startTarget", "endTarget"]
            }""", JsonObject::class.java)
        ),
        Tool(
            name = "browser_handle_dialog",
            description = "Accept or dismiss a browser dialog (alert, confirm, prompt)",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "accept": {"type": "boolean", "description": "Accept (true) or dismiss (false) the dialog"},
                    "promptText": {"type": "string", "description": "Text to enter for prompt dialogs"}
                },
                "required": ["accept"]
            }""", JsonObject::class.java)
        ),
        Tool(
            name = "browser_console_messages",
            description = "Retrieve console messages from the page",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "level": {"type": "string", "enum": ["error", "warning", "info", "debug"], "description": "Filter by console level"}
                }
            }""", JsonObject::class.java)
        ),
        Tool(
            name = "browser_resize",
            description = "Resize the browser viewport",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "width": {"type": "integer", "description": "New viewport width"},
                    "height": {"type": "integer", "description": "New viewport height"}
                },
                "required": ["width", "height"]
            }""", JsonObject::class.java)
        )
    )

    fun getToolsForApi(): List<Map<String, Any>> {
        return tools.map { tool ->
            mapOf(
                "type" to "function",
                "function" to mapOf(
                    "name" to tool.name,
                    "description" to tool.description,
                    "parameters" to tool.parameters
                )
            )
        }
    }
}
