# Datacircle MCP Server for JetBrains IDEs

Use [Datacircle](https://datacircle.dev)'s MCP server from your IDE's AI agents: AI Assistant, Junie, GitHub Copilot, Codex and Claude.

> It gets a LinkedIn profile from its URL, through Up2Data, HarvestAPI or Fetchin.

## Getting started

1. > Log in at datacircle.dev/login with your work email. Your API key is on the page once you're in.
2. Install the plugin, click **Connect** and paste your key.
3. Turn the server on when your IDE asks.

The plugin adds `https://api.datacircle.dev/mcp` to `~/.ai/mcp/mcp.json`, the MCP server list your AI agents share, with your key as a Bearer token. To change the key: **Tools | Set Datacircle API Key**. Uninstall the plugin and it removes the server.

> Sign up at datacircle.dev with your work email: a $5 credit, that's 2,105 LinkedIn profiles at $2.375 per 1,000.

## Docs

- [MCP server](https://docs.datacircle.dev/mcp-server): the tools and what each call costs
- [LinkedIn Profile API](https://docs.datacircle.dev/linkedin-profile): Up2Data, HarvestAPI and Fetchin
- [Pricing](https://docs.datacircle.dev/pricing)

## Build

```
./gradlew test buildPlugin
```

The plugin is `build/distributions/datacircle-mcp-server-<version>.zip`. With `DATACIRCLE_TEST_TOKEN=<key>`, the tests call our live server with the entry the plugin writes.

Apache License 2.0, adapted from [Postman's plugin](https://github.com/postmanlabs/postman-mcp-jetbrains-plugin) (`NOTICE`). Contact: wayne@datacircle.dev
