package com.example.tools

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

class OpenAppTool : Tool {
    override val definition = ToolDefinition(
        name = "open_app",
        description = "Launches an installed application on the user's Android phone by application name.",
        parameters = listOf(
            ToolParameter(
                name = "app_name",
                type = "STRING",
                description = "The common name of the application to launch (e.g., 'YouTube', 'Chrome', 'Settings', 'Camera', 'Calculator').",
                required = true
            )
        )
    )

    private val commonPackageMap = mapOf(
        "youtube" to listOf("com.google.android.youtube"),
        "chrome" to listOf("com.android.chrome"),
        "google chrome" to listOf("com.android.chrome"),
        "browser" to listOf("com.android.chrome"),
        "maps" to listOf("com.google.android.apps.maps"),
        "google maps" to listOf("com.google.android.apps.maps"),
        "camera" to listOf("com.android.camera2", "com.google.android.GoogleCamera", "com.android.camera"),
        "settings" to listOf("com.android.settings"),
        "gmail" to listOf("com.google.android.gm"),
        "calculator" to listOf("com.google.android.calculator", "com.android.calculator2"),
        "clock" to listOf("com.google.android.deskclock", "com.android.deskclock"),
        "alarm" to listOf("com.google.android.deskclock", "com.android.deskclock"),
        "photos" to listOf("com.google.android.apps.photos", "com.android.gallery3d"),
        "gallery" to listOf("com.google.android.apps.photos", "com.android.gallery3d"),
        "calendar" to listOf("com.google.android.calendar"),
        "spotify" to listOf("com.spotify.music"),
        "play store" to listOf("com.android.vending"),
        "playstore" to listOf("com.android.vending"),
        "contacts" to listOf("com.google.android.contacts", "com.android.contacts"),
        "messages" to listOf("com.google.android.apps.messaging", "com.android.mms"),
        "phone" to listOf("com.google.android.dialer", "com.android.dialer")
    )

    override suspend fun execute(context: Context, arguments: Map<String, String>): ToolResult {
        val appName = arguments["app_name"]?.trim() ?: return ToolResult(false, "Application name not specified.")
        val pm = context.packageManager
        val cleanName = appName.lowercase()

        // 1. Try known package mappings
        val mappedPackages = commonPackageMap[cleanName] ?: emptyList()
        for (pkg in mappedPackages) {
            try {
                val launchIntent = pm.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return ToolResult(true, "Opening $appName.")
                }
            } catch (ignored: Exception) {
            }
        }

        // 2. Query all installed launchable activities
        try {
            val mainIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            for (info in resolveInfos) {
                val label = info.loadLabel(pm).toString()
                if (label.equals(cleanName, ignoreCase = true) ||
                    label.lowercase().contains(cleanName) ||
                    cleanName.contains(label.lowercase())
                ) {
                    val pkg = info.activityInfo.packageName
                    val launchIntent = pm.getLaunchIntentForPackage(pkg)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        return ToolResult(true, "Opening $label.")
                    }
                }
            }
        } catch (e: Exception) {
            // Package visibility restriction fallback
        }

        // 3. Fallback: Try launching via generic action if applicable
        if (cleanName.contains("camera")) {
            val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (cameraIntent.resolveActivity(pm) != null) {
                context.startActivity(cameraIntent)
                return ToolResult(true, "Opening Camera.")
            }
        }

        if (cleanName.contains("settings")) {
            val settingsIntent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)
            return ToolResult(true, "Opening Settings.")
        }

        return ToolResult(
            false,
            "I couldn't find $appName on your phone. Please verify it is installed."
        )
    }
}
