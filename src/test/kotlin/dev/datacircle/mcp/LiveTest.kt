package dev.datacircle.mcp

import com.google.gson.JsonParser
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path

/**
 * On live: the entry the plugin writes, called as an MCP client calls it (tools/list on its url, with its headers),
 * and the key check. `DATACIRCLE_TEST_TOKEN=<key> ./gradlew test`; without the variable, the tests that need a key skip.
 */
class LiveTest {

    @TempDir
    lateinit var tmpDir: Path
    private lateinit var configFile: File
    private val key = System.getenv("DATACIRCLE_TEST_TOKEN").orEmpty()

    @BeforeEach
    fun setUp() {
        configFile = tmpDir.resolve("mcp.json").toFile()
        System.setProperty(McpAgentTarget.TEST_CONFIG_FILE, configFile.absolutePath)
    }

    @AfterEach
    fun tearDown() {
        System.clearProperty(McpAgentTarget.TEST_CONFIG_FILE)
    }

    /** tools/list on the url of the entry the plugin wrote, with the headers it wrote. */
    private fun toolsListFromConfig(): HttpResponse<String> {
        val entry = JsonParser.parseString(configFile.readText()).asJsonObject
            .getAsJsonObject("mcpServers").getAsJsonObject("datacircle")
        val request = HttpRequest.newBuilder(URI(entry.get("url").asString))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json, text/event-stream")
            .POST(HttpRequest.BodyPublishers.ofString("""{"jsonrpc": "2.0", "id": 1, "method": "tools/list"}"""))
        entry.getAsJsonObject("headers")?.entrySet()?.forEach { (name, value) -> request.header(name, value.asString) }
        return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString())
    }

    @Test
    fun `without a key, 401 and the OAuth sign-in`() {
        McpConfigWriter.applyConfig("").getOrThrow()

        val answer = toolsListFromConfig()

        assertEquals(401, answer.statusCode())
        val auth = answer.headers().firstValue("www-authenticate").orElse("")
        assertTrue(auth.contains("https://api.datacircle.dev/.well-known/oauth-protected-resource/mcp"), auth)
    }

    @Test
    fun `with the key, 200 and our tools`() {
        assumeTrue(key.isNotBlank(), "DATACIRCLE_TEST_TOKEN not set")
        McpConfigWriter.applyConfig(key).getOrThrow()

        val answer = toolsListFromConfig()

        assertEquals(200, answer.statusCode(), answer.body())
        assertTrue(answer.body().contains("get_linkedin_profile"), answer.body())
        val names = Regex("\"name\":\\s*\"([a-z_]+)\"").findAll(answer.body()).map { it.groupValues[1] }.toSet()
        println("tools: $names")
    }

    @Test
    fun `the key check knows the test key and refuses a wrong one`() {
        assumeTrue(key.isNotBlank(), "DATACIRCLE_TEST_TOKEN not set")

        assertTrue(KeyCheck.check(key).isSuccess)
        val wrong = KeyCheck.check("not-a-key")
        assertEquals("We don't know this key.", wrong.exceptionOrNull()?.message)
    }
}
