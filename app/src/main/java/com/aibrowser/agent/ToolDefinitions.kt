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
            description = "Capture accessibility snapshot of the current page. Returns structured DOM tree with element references for actions.",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "depth": {"type": "integer", "description": "Limit depth of snapshot tree"},
                    "boxes": {"type": "boolean", "description": "Include bounding boxes for elements"}
                }
            }""", JsonObject::class.java)
        ),
        Tool(
            name = "browser_take_screenshot",
            description = "Take a screenshot of the current page.",
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
                    "target": {"type": "string", "description": "CSS selector for element"},
                    "doubleClick": {"type": "boolean", "description": "Perform double click"},
                    "button": {"type": "string", "enum": ["left", "right", "middle"], "description": "Mouse button"}
                },
                "required": ["target"]
            }""", JsonObject::class.java)
        ),
        Tool(
            name = "browser_type",
            description = "Type text into an editable element",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "target": {"type": "string", "description": "CSS selector"},
                    "text": {"type": "string", "description": "Text to type"},
                    "submit": {"type": "boolean", "description": "Press Enter after typing"}
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
                                "selector": {"type": "string"},
                                "value": {"type": "string"}
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
                    "target": {"type": "string", "description": "CSS selector"},
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
                    "target": {"type": "string", "description": "CSS selector"}
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
                    "key": {"type": "string", "description": "Key name: Enter, Tab, Escape, etc."}
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
                    "function": {"type": "string", "description": "JS function: () => { return document.title; }"}
                },
                "required": ["function"]
            }""", JsonObject::class.java)
        ),
        Tool(
            name = "browser_wait_for",
            description = "Wait for text to appear/disappear or time to pass",
            parameters = gson.fromJson("""{
                "type": "object",
                "properties": {
                    "text": {"type": "string", "description": "Text to wait for"},
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
