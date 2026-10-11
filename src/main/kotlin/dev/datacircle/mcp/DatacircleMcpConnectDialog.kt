// Adapted from Postman's postman-mcp-jetbrains-plugin (Apache License 2.0, Copyright 2026 Postman, Inc.).
package dev.datacircle.mcp

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class DatacircleMcpConnectDialog(
    project: Project?,
    private val onConnect: (apiKey: String) -> Result<Unit>,
) : DialogWrapper(project) {

    private val apiKeyField = JBPasswordField()
    private val errorLabel = JBLabel("").apply { foreground = JBColor.RED; isVisible = false }

    init {
        title = "Connect Datacircle"
        setOKButtonText("Connect")
        setCancelButtonText("Not now")
        init()
    }

    override fun createCenterPanel(): JComponent = panel {
        row {
            label(
                "<html>Log in at datacircle.dev/login with your work email.<br>" +
                    "Your API key is on the page once you're in.</html>"
            )
        }
        row("API key:") {
            cell(apiKeyField).columns(COLUMNS_LARGE)
        }
        row {
            cell(errorLabel)
        }
        row {
            link("Get your API key") { BrowserUtil.browse(LOGIN_URL) }
        }
    }

    override fun doOKAction() {
        val apiKey = String(apiKeyField.password).trim()
        if (apiKey.isBlank()) {
            showError("Paste your API key.")
            return
        }
        val result = onConnect(apiKey)
        if (result.isSuccess) {
            super.doOKAction()
        } else {
            showError(result.exceptionOrNull()?.message ?: "Could not connect.")
        }
    }

    private fun showError(message: String) {
        errorLabel.text = message
        errorLabel.isVisible = true
        pack()
    }

    override fun getPreferredFocusedComponent(): JComponent = apiKeyField

    companion object {
        const val LOGIN_URL = "https://datacircle.dev/login"
    }
}
