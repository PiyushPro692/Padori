package com.example.tools

import android.content.Context
import android.content.Intent
import android.provider.Settings

class OpenSettingsTool : Tool {
    override val definition = ToolDefinition(
        name = "open_settings",
        description = "Opens the requested Android system settings screen (e.g., wifi, bluetooth, display, sound, battery, apps).",
        parameters = listOf(
            ToolParameter(
                name = "section",
                type = "STRING",
                description = "Settings category: 'wifi', 'bluetooth', 'display', 'sound', 'battery', 'apps', 'location', or 'general'.",
                required = false
            )
        )
    )

    override suspend fun execute(context: Context, arguments: Map<String, String>): ToolResult {
        val section = arguments["section"]?.lowercase()?.trim() ?: "general"

        val action = when {
            section.contains("wifi") || section.contains("wi-fi") || section.contains("network") ->
                Settings.ACTION_WIFI_SETTINGS
            section.contains("bluetooth") ->
                Settings.ACTION_BLUETOOTH_SETTINGS
            section.contains("display") || section.contains("brightness") || section.contains("screen") ->
                Settings.ACTION_DISPLAY_SETTINGS
            section.contains("sound") || section.contains("volume") || section.contains("audio") ->
                Settings.ACTION_SOUND_SETTINGS
            section.contains("battery") || section.contains("power") ->
                Settings.ACTION_BATTERY_SAVER_SETTINGS
            section.contains("app") || section.contains("application") ->
                Settings.ACTION_APPLICATION_SETTINGS
            section.contains("locat") || section.contains("gps") ->
                Settings.ACTION_LOCATION_SOURCE_SETTINGS
            section.contains("date") || section.contains("clock") ->
                Settings.ACTION_DATE_SETTINGS
            else ->
                Settings.ACTION_SETTINGS
        }

        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolResult(true, "Opening $section settings.")
        } catch (e: Exception) {
            ToolResult(false, "Could not open settings: ${e.localizedMessage}")
        }
    }
}
