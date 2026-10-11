package dev.datacircle.mcp

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/** Asks our API whether it knows a key before the plugin writes it anywhere. */
internal object KeyCheck {

    const val BALANCE_URL = "https://api.datacircle.dev/balance/"

    fun check(apiKey: String, url: String = BALANCE_URL): Result<Unit> = runCatching {
        val request = HttpRequest.newBuilder(URI(url))
            .header("Authorization", "Bearer ${apiKey.trim()}")
            .timeout(Duration.ofSeconds(20))
            .GET()
            .build()
        val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
        val status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode()
        when (status) {
            200 -> Unit
            401, 403 -> error("We don't know this key.")
            else -> error("Our server returned error $status. Try again in a minute.")
        }
    }
}
