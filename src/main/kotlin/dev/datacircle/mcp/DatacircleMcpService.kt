// Adapted from Postman's postman-mcp-jetbrains-plugin (Apache License 2.0, Copyright 2026 Postman, Inc.).
package dev.datacircle.mcp

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem

/** The API key, kept in the IDE's password safe, and our entry in the MCP config. */
@Service(Service.Level.APP)
class DatacircleMcpService {

    private val log = logger<DatacircleMcpService>()
    private val credentialAttributes = CredentialAttributes(generateServiceName("Datacircle MCP Server", "apiKey"))

    fun getApiKey(): String = PasswordSafe.instance.getPassword(credentialAttributes) ?: ""

    fun setApiKey(apiKey: String) {
        PasswordSafe.instance.set(
            credentialAttributes,
            if (apiKey.isBlank()) null else Credentials("apiKey", apiKey.trim()),
        )
    }

    fun applyConfig(): Result<Unit> {
        val result = McpConfigWriter.applyConfig(getApiKey())
        result.onSuccess {
            log.info("Datacircle MCP config written")
            // So the IDE's MCP client sees the change without a restart.
            LocalFileSystem.getInstance().refreshIoFiles(McpAgentTarget.ALL.map { it.getConfigFile() })
        }.onFailure { e ->
            log.warn("Could not write the Datacircle MCP config", e)
            notify(null, "Datacircle MCP Server", "Could not write ~/.ai/mcp/mcp.json: ${e.message}", NotificationType.ERROR)
        }
        return result
    }

    fun removeConfig(): Result<Unit> =
        McpConfigWriter.removeConfig().onFailure { e -> log.warn("Could not remove the Datacircle MCP config", e) }

    fun notify(project: Project?, title: String, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP)
            .createNotification(title, content, type)
            .notify(project)
    }

    companion object {
        const val NOTIFICATION_GROUP = "Datacircle MCP Server"

        fun getInstance(): DatacircleMcpService = service()
    }
}
