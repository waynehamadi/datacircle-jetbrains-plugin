// Adapted from Postman's postman-mcp-jetbrains-plugin (Apache License 2.0, Copyright 2026 Postman, Inc.).
package dev.datacircle.mcp

import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor

/**
 * Installed or turned back on without a restart: our entry written again if there is a key.
 * Turned off or uninstalled: our entry taken out of the config, and the key forgotten.
 */
class DatacircleMcpPluginLifecycleListener : DynamicPluginListener {

    override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
        if (pluginDescriptor.pluginId.idString != PLUGIN_ID) return
        val service = DatacircleMcpService.getInstance()
        if (service.getApiKey().isNotBlank()) service.applyConfig()
    }

    override fun beforePluginUnload(pluginDescriptor: IdeaPluginDescriptor, isUpdate: Boolean) {
        if (pluginDescriptor.pluginId.idString != PLUGIN_ID || isUpdate) return
        val service = DatacircleMcpService.getInstance()
        service.removeConfig()
        service.setApiKey("")
    }

    companion object {
        const val PLUGIN_ID = "dev.datacircle.mcp-server"
    }
}
