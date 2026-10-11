package dev.datacircle.mcp

import com.google.gson.JsonParser
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class McpConfigWriterTest {

    @TempDir
    lateinit var tmpDir: Path
    private lateinit var configFile: File

    @BeforeEach
    fun setUp() {
        configFile = tmpDir.resolve(".ai/mcp/mcp.json").toFile()
        System.setProperty(McpAgentTarget.TEST_CONFIG_FILE, configFile.absolutePath)
    }

    @AfterEach
    fun tearDown() {
        System.clearProperty(McpAgentTarget.TEST_CONFIG_FILE)
    }

    private fun servers() = JsonParser.parseString(configFile.readText()).asJsonObject.getAsJsonObject("mcpServers")

    @Test
    fun `writes our url and the key as a Bearer header, the folder made`() {
        McpConfigWriter.applyConfig("  k123 ").getOrThrow()

        val ours = servers().getAsJsonObject("datacircle")
        assertEquals("https://api.datacircle.dev/mcp", ours.get("url").asString)
        assertEquals("Bearer k123", ours.getAsJsonObject("headers").get("Authorization").asString)
        assertEquals(setOf("url", "headers"), ours.keySet())
    }

    @Test
    fun `without a key, the url alone`() {
        McpConfigWriter.applyConfig("").getOrThrow()

        assertEquals(setOf("url"), servers().getAsJsonObject("datacircle").keySet())
    }

    @Test
    fun `keeps the other servers and the file's other keys as they were`() {
        configFile.parentFile.mkdirs()
        configFile.writeText(
            """{"mcpServers": {"other": {"command": "node", "args": ["a.js"], "timeout": 5000, "env": {"X": null}}}, "inputs": [1, 2.5]}"""
        )

        McpConfigWriter.applyConfig("k").getOrThrow()

        val text = configFile.readText()
        assertTrue(text.contains("\"timeout\": 5000,") || text.contains("\"timeout\": 5000\n"), text)
        assertTrue(text.contains("\"X\": null"), text)
        assertTrue(text.contains("2.5"), text)
        assertEquals(setOf("other", "datacircle"), servers().keySet())
        assertEquals("a.js", servers().getAsJsonObject("other").getAsJsonArray("args")[0].asString)
    }

    @Test
    fun `a new key replaces the old one`() {
        McpConfigWriter.applyConfig("old").getOrThrow()
        McpConfigWriter.applyConfig("new").getOrThrow()

        assertEquals("Bearer new", servers().getAsJsonObject("datacircle").getAsJsonObject("headers").get("Authorization").asString)
    }

    @Test
    fun `never writes over a file it can't read`() {
        configFile.parentFile.mkdirs()
        val handEdited = """{"mcpServers": {"other": {"url": "https://x"}},, }"""
        configFile.writeText(handEdited)

        assertTrue(McpConfigWriter.applyConfig("k").isFailure)
        assertEquals(handEdited, configFile.readText())
    }

    @Test
    fun `remove takes out ours alone`() {
        configFile.parentFile.mkdirs()
        configFile.writeText("""{"mcpServers": {"other": {"url": "https://x"}}}""")
        McpConfigWriter.applyConfig("k").getOrThrow()

        McpConfigWriter.removeConfig().getOrThrow()

        assertEquals(setOf("other"), servers().keySet())
    }

    @Test
    fun `remove with ours the only server leaves an empty object`() {
        McpConfigWriter.applyConfig("k").getOrThrow()

        McpConfigWriter.removeConfig().getOrThrow()

        assertEquals("{}", configFile.readText().trim())
    }

    @Test
    fun `remove with no file makes none`() {
        McpConfigWriter.removeConfig().getOrThrow()

        assertFalse(configFile.exists())
    }
}
