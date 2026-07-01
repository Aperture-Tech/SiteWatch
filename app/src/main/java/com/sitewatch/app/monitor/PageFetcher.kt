package com.sitewatch.app.monitor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Fetches the raw HTML body of a URL. */
@Singleton
class PageFetcher @Inject constructor(
    private val client: OkHttpClient,
) {
    /**
     * Performs a GET and returns the response body as a string.
     *
     * @throws IOException on network failure or a non-2xx response.
     */
    suspend fun fetch(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(normalize(url))
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} for $url")
            }
            response.body?.string() ?: throw IOException("Empty response body for $url")
        }
    }

    private fun normalize(url: String): String =
        if (url.startsWith("http://", ignoreCase = true) ||
            url.startsWith("https://", ignoreCase = true)
        ) {
            url
        } else {
            "https://$url"
        }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Android; SiteWatch) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
    }
}
