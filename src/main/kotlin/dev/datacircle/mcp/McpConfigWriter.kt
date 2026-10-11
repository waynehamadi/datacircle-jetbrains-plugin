// Adapted from Postman's postman-mcp-jetbrains-plugin (Apache License 2.0, Copyright 2026 Postman, Inc.).
// Ours writes a remote server (a URL and a Bearer header) where Postman's runs a local one.
package dev.datacircle.mcp

import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * Reads and writes the MCP config files. No IntelliJ platform class, so tests run without an IDE.
 *
 * Our entry is `"datacircle": {"url": "https://api.datacircle.dev/mcp", "headers": {"Authorization": "Bearer <key>"}}`.
 * The other servers in the file are kept as they are.
 */
internal object McpConfigWriter {

    const val SERVER_NAME = "datacircle"
    const val SERVER_URL = "https://api.datacircle.dev/mcp"

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .serializeNulls()
        // 5000 stays 5000, not 5000.0, in the servers we don't own.
        .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
        .create()

    fun applyConfig(
        apiKey: String,
        targets: List<McpAgentTarget> = McpAgentTarget.ALL,
    ): Result<Unit> = runCatching {
        val entry = buildEntry(apiKey)
        for (target in targets) {
            val configFile = target.getConfigFile()
            val config = readExistingConfig(configFile)
            @Suppress("UNCHECKED_CAST")
            val servers = config[target.rootKey] as? MutableMap<String, Any?> ?: linkedMapOf()
            servers[SERVER_NAME] = entry
            config[target.rootKey] = servers
            writeConfig(configFile, config)
        }
    }

    fun removeConfig(
        targets: List<McpAgentTarget> = McpAgentTarget.ALL,
    ): Result<Unit> = runCatching {
        for (target in targets) {
            val configFile = target.getConfigFile()
            if (!configFile.exists()) continue
            val config = readExistingConfig(configFile)
            @Suppress("UNCHECKED_CAST")
            val servers = config[target.rootKey] as? MutableMap<String, Any?> ?: continue
            if (servers.remove(SERVER_NAME) == null) continue
            if (servers.isEmpty()) config.remove(target.rootKey)
            writeConfig(configFile, config)
        }
    }

    internal fun buildEntry(apiKey: String): Map<String, Any> {
        val entry = linkedMapOf<String, Any>("url" to SERVER_URL)
        if (apiKey.isNotBlank()) entry["headers"] = mapOf("Authorization" to "Bearer ${apiKey.trim()}")
        return entry
    }

    /** Throws on a file it can't read, so a hand-edited file is never written over. */
    internal fun readExistingConfig(file: File): MutableMap<String, Any?> {
        if (!file.exists()) return linkedMapOf()
        val text = file.readText()
        if (text.isBlank()) return linkedMapOf()
        val type = object : TypeToken<LinkedHashMap<String, Any?>>() {}.type
        return gson.fromJson<LinkedHashMap<String, Any?>>(text, type)
            ?: throw IllegalStateException("${file.path} isn't a JSON object")
    }

    private fun writeConfig(file: File, config: Map<String, Any?>) {
        file.parentFile?.mkdirs()
        file.writeText(gson.toJson(config) + "\n")
    }
}
