package dev.datacircle.mcp

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.notification.NotificationGroupManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.startup.StartupActivity
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files

/** The plugin loaded in a headless IDE: its plugin.xml, the key in the password safe, the config it writes and takes out. */
class PluginInIdeTest : BasePlatformTestCase() {

    private lateinit var configFile: File

    override fun setUp() {
        super.setUp()
        configFile = Files.createTempDirectory("mcp").resolve("mcp.json").toFile()
        System.setProperty(McpAgentTarget.TEST_CONFIG_FILE, configFile.absolutePath)
    }

    override fun tearDown() {
        try {
            DatacircleMcpService.getInstance().setApiKey("")
            System.clearProperty(McpAgentTarget.TEST_CONFIG_FILE)
        } finally {
            super.tearDown()
        }
    }

    private val plugin get() = PluginManagerCore.getPlugin(PluginId.getId(DatacircleMcpPluginLifecycleListener.PLUGIN_ID))!!

    fun testPluginXmlRegistersItsParts() {
        assertEquals("Datacircle MCP Server", plugin.name)
        assertNotNull(ActionManager.getInstance().getAction("dev.datacircle.mcp.SetupAction"))
        assertNotNull(NotificationGroupManager.getInstance().getNotificationGroup(DatacircleMcpService.NOTIFICATION_GROUP))
        val activities = StartupActivity.POST_STARTUP_ACTIVITY.extensionList.map { it.javaClass.name }
        assertTrue(activities.toString(), activities.contains(DatacircleMcpStartupActivity::class.java.name))
    }

    fun testStartupWritesTheEntryWithTheKeptKey() {
        val service = DatacircleMcpService.getInstance()
        service.setApiKey(" k1 ")
        assertEquals("k1", service.getApiKey())

        runBlocking { DatacircleMcpStartupActivity().execute(project) }

        val text = configFile.readText()
        assertTrue(text, text.contains("\"url\": \"https://api.datacircle.dev/mcp\""))
        assertTrue(text, text.contains("\"Authorization\": \"Bearer k1\""))
    }

    fun testStartupWithoutAKeyWritesNothing() {
        runBlocking { DatacircleMcpStartupActivity().execute(project) }

        assertFalse(configFile.exists())
    }

    fun testUninstallTakesOutTheEntryAndForgetsTheKey() {
        configFile.writeText("""{"mcpServers": {"other": {"url": "https://x"}}}""")
        val service = DatacircleMcpService.getInstance()
        service.setApiKey("k2")
        assertTrue(service.applyConfig().isSuccess)

        DatacircleMcpPluginLifecycleListener().beforePluginUnload(plugin, isUpdate = false)

        assertEquals("", service.getApiKey())
        val text = configFile.readText()
        assertFalse(text, text.contains("datacircle"))
        assertTrue(text, text.contains("\"other\""))
    }

    fun testAnUpdateKeepsBoth() {
        val service = DatacircleMcpService.getInstance()
        service.setApiKey("k3")
        assertTrue(service.applyConfig().isSuccess)

        DatacircleMcpPluginLifecycleListener().beforePluginUnload(plugin, isUpdate = true)

        assertEquals("k3", service.getApiKey())
        assertTrue(configFile.readText().contains("Bearer k3"))
    }
}
