// Adapted from Postman's postman-mcp-jetbrains-plugin (Apache License 2.0, Copyright 2026 Postman, Inc.).
package dev.datacircle.mcp

import java.io.File

/**
 * A config file that the IDE's AI agents read their MCP servers from.
 *
 * All JetBrains AI features (AI Assistant, Junie, Codex, Claude, GitHub Copilot for JetBrains)
 * share one file, ~/.ai/mcp/mcp.json: the registry key "llm.mcp.client.global.mcp.json.path"
 * (default .ai/mcp/mcp.json), relative to the user's home.
 */
sealed class McpAgentTarget(
    val displayName: String,
    /** The key this file's servers are under. */
    val rootKey: String,
) {
    abstract fun getConfigFile(): File

    object JetBrains : McpAgentTarget(
        displayName = "All JetBrains AI agents (AI Assistant, Junie, GitHub Copilot, Codex, Claude)",
        rootKey = "mcpServers",
    ) {
        override fun getConfigFile(): File {
            // Tests point it at a temporary file, never at ~/.ai/mcp/mcp.json.
            val override = System.getProperty(TEST_CONFIG_FILE)
            if (override != null) return File(override)
            return File(System.getProperty("user.home"), ".ai/mcp/mcp.json")
        }
    }

    companion object {
        const val TEST_CONFIG_FILE = "dev.datacircle.mcp.test.configFile"

        // A getter, so JetBrains isn't null while the class loader initializes it.
        val ALL: List<McpAgentTarget> get() = listOf(JetBrains)
    }
}
