package com.example.tools

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class GetTimeTool : Tool {
    override val definition = ToolDefinition(
        name = "get_time",
        description = "Returns current local time formatted in hours, minutes, and AM/PM.",
        parameters = emptyList()
    )

    override suspend fun execute(context: Context, arguments: Map<String, String>): ToolResult {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        val currentTime = sdf.format(Date())
        return ToolResult(
            success = true,
            message = "It is currently $currentTime."
        )
    }
}

class GetDateTool : Tool {
    override val definition = ToolDefinition(
        name = "get_date",
        description = "Returns current day of the week, calendar date, month, and year.",
        parameters = emptyList()
    )

    override suspend fun execute(context: Context, arguments: Map<String, String>): ToolResult {
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        val currentDate = sdf.format(Date())
        return ToolResult(
            success = true,
            message = "Today is $currentDate."
        )
    }
}

class SetAlarmTool : Tool {
    override val definition = ToolDefinition(
        name = "set_alarm",
        description = "Sets an alarm in the Android clock application for a given time (e.g. 7:00 AM).",
        parameters = listOf(
            ToolParameter(
                name = "time",
                type = "STRING",
                description = "Alarm time such as '7:00 AM', '07:30', or '14:00'.",
                required = true
            ),
            ToolParameter(
                name = "label",
                type = "STRING",
                description = "Optional label/message for the alarm.",
                required = false
            )
        )
    )

    override suspend fun execute(context: Context, arguments: Map<String, String>): ToolResult {
        val timeStr = arguments["time"]?.trim() ?: return ToolResult(false, "Alarm time not specified.")
        val label = arguments["label"] ?: "JARVIS Alarm"

        var hour = 7
        var minutes = 0

        try {
            val clean = timeStr.lowercase().replace("alarm", "").trim()
            val isPM = clean.contains("pm")
            val isAM = clean.contains("am")
            val digitsOnly = clean.replace("am", "").replace("pm", "").trim()

            if (digitsOnly.contains(":")) {
                val parts = digitsOnly.split(":")
                hour = parts[0].trim().toInt()
                minutes = parts[1].trim().toInt()
            } else {
                hour = digitsOnly.toInt()
                minutes = 0
            }

            if (isPM && hour < 12) hour += 12
            if (isAM && hour == 12) hour = 0
        } catch (e: Exception) {
            // Default 7:00 AM fallback
            hour = 7
            minutes = 0
        }

        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minutes)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minutes)
            ToolResult(true, "Setting alarm for $formattedTime.")
        } catch (e: Exception) {
            ToolResult(false, "Android permission or clock app limitation: ${e.localizedMessage}")
        }
    }
}
