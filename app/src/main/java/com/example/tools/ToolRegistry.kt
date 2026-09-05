package com.example.tools

import android.content.Context
import java.util.concurrent.ConcurrentHashMap

class ToolRegistry(private val context: Context) {
    private val registeredTools = ConcurrentHashMap<String, Tool>()

    init {
        // Register core tools
        register(OpenAppTool())
        register(OpenWebsiteTool())
        register(WebSearchTool())
        register(BatteryStatusTool())
        register(DeviceInfoTool())
        register(CameraTool())
        register(GetTimeTool())
        register(GetDateTool())
        register(SetAlarmTool())
        register(OpenSettingsTool())
        register(MakeCallTool())
        register(SendMessageTool())
    }

    fun register(tool: Tool) {
        registeredTools[tool.definition.name] = tool
    }

    fun unregister(toolName: String) {
        registeredTools.remove(toolName)
    }

    fun getTool(toolName: String): Tool? {
        return registeredTools[toolName]
    }

    fun getAllTools(): List<Tool> {
        return registeredTools.values.toList()
    }

    suspend fun executeTool(toolName: String, arguments: Map<String, String>): ToolResult {
        val tool = registeredTools[toolName]
            ?: return ToolResult(
                success = false,
                message = "The requested action '$toolName' is not supported by JARVIS on this device."
            )

        val validationError = tool.validate(arguments)
        if (validationError != null) {
            return ToolResult(
                success = false,
                message = "Action validation failed: $validationError"
            )
        }

        return try {
            tool.execute(context, arguments)
        } catch (e: Exception) {
            ToolResult(
                success = false,
                message = "Error executing '$toolName': ${e.localizedMessage ?: "Unknown system exception"}"
            )
        }
    }

    /**
     * Builds a text summary of available tools for LLM system prompts.
     */
    fun buildSystemPromptToolDescriptions(): String {
        val sb = StringBuilder()
        sb.append("AVAILABLE TOOLS:\n")
        for (tool in registeredTools.values) {
            val def = tool.definition
            sb.append("- ${def.name}: ${def.description}\n")
            if (def.parameters.isNotEmpty()) {
                sb.append("  Arguments:\n")
                for (param in def.parameters) {
                    val req = if (param.required) "required" else "optional"
                    sb.append("    * ${param.name} (${param.type}, $req): ${param.description}\n")
                }
            } else {
                sb.append("  Arguments: none\n")
            }
        }
        return sb.toString()
    }
}
