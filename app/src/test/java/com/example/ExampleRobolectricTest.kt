package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.FallbackLocalProvider
import com.example.tools.ToolRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("JARVIS", appName)
    }

    @Test
    fun `test intent resolution for open youtube`() = runBlocking {
        val provider = FallbackLocalProvider()
        val result = provider.processUserPrompt(
            userPrompt = "Open YouTube",
            conversationHistory = emptyList(),
            memories = emptyList(),
            availableToolsDescription = "",
            apiKey = ""
        )
        assertEquals("open_app", result.toolName)
        assertEquals("YouTube", result.toolArguments["app_name"])
        assertEquals("Opening YouTube.", result.spokenResponse)
    }

    @Test
    fun `test intent resolution for search google`() = runBlocking {
        val provider = FallbackLocalProvider()
        val result = provider.processUserPrompt(
            userPrompt = "Search Google for Minecraft shaders",
            conversationHistory = emptyList(),
            memories = emptyList(),
            availableToolsDescription = "",
            apiKey = ""
        )
        assertEquals("web_search", result.toolName)
        assertEquals("Minecraft shaders", result.toolArguments["query"])
    }

    @Test
    fun `test intent resolution for battery and time`() = runBlocking {
        val provider = FallbackLocalProvider()
        val batteryResult = provider.processUserPrompt(
            userPrompt = "What is my battery percentage?",
            conversationHistory = emptyList(),
            memories = emptyList(),
            availableToolsDescription = "",
            apiKey = ""
        )
        assertEquals("get_battery_status", batteryResult.toolName)

        val timeResult = provider.processUserPrompt(
            userPrompt = "What time is it?",
            conversationHistory = emptyList(),
            memories = emptyList(),
            availableToolsDescription = "",
            apiKey = ""
        )
        assertEquals("get_time", timeResult.toolName)
    }

    @Test
    fun `test tool registry execution`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val registry = ToolRegistry(context)

        val timeResult = registry.executeTool("get_time", emptyMap())
        assertTrue(timeResult.success)
        assertTrue(timeResult.message.contains("currently"))

        val batteryResult = registry.executeTool("get_battery_status", emptyMap())
        assertTrue(batteryResult.success)
        assertNotNull(batteryResult.data["percentage"])
    }
}

