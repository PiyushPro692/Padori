package com.example.tools

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.provider.MediaStore

class BatteryStatusTool : Tool {
    override val definition = ToolDefinition(
        name = "get_battery_status",
        description = "Retrieves current device battery percentage, charging state, and power status.",
        parameters = emptyList()
    )

    override suspend fun execute(context: Context, arguments: Map<String, String>): ToolResult {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, ifilter)

        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        val batteryPct = if (level >= 0 && scale > 0) {
            (level * 100 / scale.toFloat()).toInt()
        } else {
            100
        }

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val chargingState = if (isCharging) "and is currently charging" else "and is not charging"
        val message = "Your battery is at $batteryPct percent $chargingState."

        return ToolResult(
            success = true,
            message = message,
            data = mapOf("percentage" to batteryPct, "isCharging" to isCharging)
        )
    }
}

class DeviceInfoTool : Tool {
    override val definition = ToolDefinition(
        name = "get_device_info",
        description = "Returns device model, manufacturer, and operating system build information.",
        parameters = emptyList()
    )

    override suspend fun execute(context: Context, arguments: Map<String, String>): ToolResult {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        val androidVersion = Build.VERSION.RELEASE
        val sdkVersion = Build.VERSION.SDK_INT

        val message = "Device: $manufacturer $model running Android version $androidVersion (API level $sdkVersion)."
        return ToolResult(
            success = true,
            message = message,
            data = mapOf(
                "manufacturer" to manufacturer,
                "model" to model,
                "androidVersion" to androidVersion,
                "sdk" to sdkVersion
            )
        )
    }
}

class CameraTool : Tool {
    override val definition = ToolDefinition(
        name = "open_camera",
        description = "Launches the default camera application to view viewfinder or take photos.",
        parameters = emptyList()
    )

    override suspend fun execute(context: Context, arguments: Map<String, String>): ToolResult {
        return try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                ToolResult(true, "Opening camera.")
            } else {
                // Fallback to general camera intent
                val fallbackIntent = Intent("android.media.action.STILL_IMAGE_CAMERA").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                ToolResult(true, "Launching camera system.")
            }
        } catch (e: Exception) {
            ToolResult(false, "Could not launch camera: ${e.localizedMessage}")
        }
    }
}
