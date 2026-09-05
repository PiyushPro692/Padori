package com.example.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

class OpenWebsiteTool : Tool {
    override val definition = ToolDefinition(
        name = "open_website",
        description = "Opens a web page or URL in the default web browser.",
        parameters = listOf(
            ToolParameter(
                name = "url",
                type = "STRING",
                description = "The URL of the website to open (e.g., 'https://www.google.com' or 'wikipedia.org').",
                required = true
            )
        )
    )

    override suspend fun execute(context: Context, arguments: Map<String, String>): ToolResult {
        var rawUrl = arguments["url"]?.trim() ?: return ToolResult(false, "URL not specified.")

        // URL validation & formatting
        if (!rawUrl.startsWith("http://", ignoreCase = true) && !rawUrl.startsWith("https://", ignoreCase = true)) {
            rawUrl = "https://$rawUrl"
        }

        return try {
            val uri = Uri.parse(rawUrl)
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolResult(true, "Opening website: $rawUrl")
        } catch (e: Exception) {
            ToolResult(false, "Unable to open website: ${e.localizedMessage ?: "Invalid URL format"}")
        }
    }
}

class WebSearchTool : Tool {
    override val definition = ToolDefinition(
        name = "web_search",
        description = "Searches the web using Google for a given query and displays the results in the browser.",
        parameters = listOf(
            ToolParameter(
                name = "query",
                type = "STRING",
                description = "The search phrase or question to query (e.g. 'Minecraft shaders', 'weather today').",
                required = true
            )
        )
    )

    override suspend fun execute(context: Context, arguments: Map<String, String>): ToolResult {
        val query = arguments["query"]?.trim() ?: return ToolResult(false, "Search query was empty.")

        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "https://www.google.com/search?q=$encodedQuery"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolResult(true, "Searching Google for $query.")
        } catch (e: Exception) {
            ToolResult(false, "Failed to initiate web search: ${e.localizedMessage}")
        }
    }
}
