package dev.datacircle.mcp

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/** On each project's opening: our entry written again with the key, or a notification to connect. */
class DatacircleMcpStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val service = DatacircleMcpService.getInstance()
        if (service.getApiKey().isBlank()) showConnectNotification(project) else service.applyConfig()
    }
}

/** Tools | Set Datacircle API Key. */
class DatacircleMcpSetupAction : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) = showConnectDialog(e.project)

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}

internal fun showConnectNotification(project: Project) {
    NotificationGroupManager.getInstance()
        .getNotificationGroup(DatacircleMcpService.NOTIFICATION_GROUP)
        .createNotification(
            "Connect Datacircle",
            "Add your Datacircle API key and your AI agents get our MCP server.",
            NotificationType.INFORMATION,
        )
        .addAction(NotificationAction.createSimpleExpiring("Connect") { showConnectDialog(project) })
        .notify(project)
}

internal fun showConnectDialog(project: Project?) {
    DatacircleMcpConnectDialog(project) { apiKey ->
        val result = ProgressManager.getInstance().runProcessWithProgressSynchronously<Result<Unit>, Exception>(
            { connect(apiKey) },
            "Connecting Datacircle",
            true,
            project,
        )
        result.onSuccess {
            DatacircleMcpService.getInstance().notify(
                project,
                "Datacircle MCP Server is ready",
                "Turn it on when your IDE asks. If it doesn't ask, restart the IDE.",
                NotificationType.INFORMATION,
            )
        }
    }.show()
}

/** Off the UI thread: the key checked on our API, kept, then written to the config. */
private fun connect(apiKey: String): Result<Unit> = KeyCheck.check(apiKey).mapCatching {
    val service = DatacircleMcpService.getInstance()
    val previousKey = service.getApiKey()
    service.setApiKey(apiKey)
    service.applyConfig().getOrElse {
        service.setApiKey(previousKey)
        throw it
    }
}
