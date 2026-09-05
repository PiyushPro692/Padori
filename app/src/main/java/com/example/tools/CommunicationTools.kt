package com.example.tools

import android.content.Context
import android.content.Intent
import android.net.Uri

class MakeCallTool : Tool {
    override val definition = ToolDefinition(
        name = "make_phone_call",
        description = "Opens the device dialer with the requested phone number pre-filled. Requires user tap to dial for security.",
        parameters = listOf(
            ToolParameter(
                name = "number",
                type = "STRING",
                description = "Phone number to prepare in dialer.",
                required = true
            )
        )
    )

    override suspend fun execute(context: Context, arguments: Map<String, String>): ToolResult {
        val number = arguments["number"]?.trim() ?: return ToolResult(false, "Phone number required.")
        return try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolResult(true, "Preparing phone call to $number in dialer.")
        } catch (e: Exception) {
            ToolResult(false, "Could not open dialer: ${e.localizedMessage}")
        }
    }
}

class SendMessageTool : Tool {
    override val definition = ToolDefinition(
        name = "send_text_message",
        description = "Opens SMS application with recipient and message drafted for user confirmation.",
        parameters = listOf(
            ToolParameter(
                name = "recipient",
                type = "STRING",
                description = "Phone number or recipient name.",
                required = true
            ),
            ToolParameter(
                name = "message",
                type = "STRING",
                description = "Text content of the message.",
                required = true
            )
        )
    )

    override suspend fun execute(context: Context, arguments: Map<String, String>): ToolResult {
        val recipient = arguments["recipient"]?.trim() ?: ""
        val message = arguments["message"]?.trim() ?: ""

        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$recipient")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolResult(true, "Drafted message to $recipient.")
        } catch (e: Exception) {
            ToolResult(false, "Could not draft SMS: ${e.localizedMessage}")
        }
    }
}
