package com.example.tools

import android.content.Context

data class ToolParameter(
    val name: String,
    val type: String, // "STRING", "NUMBER", "BOOLEAN"
    val description: String,
    val required: Boolean = true
)

data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: List<ToolParameter> = emptyList()
)

data class ToolResult(
    val success: Boolean,
    val message: String,
    val data: Map<String, Any?> = emptyMap()
)

interface Tool {
    val definition: ToolDefinition

    fun validate(arguments: Map<String, String>): String? {
        for (param in definition.parameters) {
            if (param.required && (!arguments.containsKey(param.name) || arguments[param.name].isNullOrBlank())) {
                return "Missing required parameter: ${param.name}"
            }
        }
        return null
    }

    suspend fun execute(context: Context, arguments: Map<String, String>): ToolResult
}
