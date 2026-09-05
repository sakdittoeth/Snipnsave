package app.snips.data

import java.net.URI
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Fetches the two things §5 lists, in its order.
 *
 * §9's constraints are the shape of this class: it only ever requests a URL
 * the reader explicitly saved, it keeps five metadata fields, and it never
 * stores article body text. The response is parsed and dropped.
 */
class MetadataClient(private val http: OkHttpClient = defaultClient()) {

    fun fetch(url: String): ArticleMetadata? {
        val seed = runCatching { fetchSubstackPost(url) }.getOrNull() ?: ArticleMetadata()
        val html = runCatching { fetchHtml(url) }.getOrNull()

        return when {
            html != null -> parseHtmlMetadata(html, url, seed)
            // The page didn't load but the API did — better than nothing.
            !seed.isEmpty -> seed
            else -> null
        }
    }

    private fun fetchHtml(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml")
            .build()

        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val type = response.header("Content-Type").orEmpty()
            if (!type.contains("html", ignoreCase = true)) return null
            // Metadata lives in <head>. Capping the read keeps a long article
            // from being pulled into memory when we want five short strings.
            return response.body?.source()?.let { source ->
                source.request(MAX_HTML_BYTES)
                // snapshot() counts in Int; the cap keeps this well inside range.
                val head = minOf(source.buffer.size, MAX_HTML_BYTES).toInt()
                source.buffer.snapshot(head).utf8()
            }
        }
    }

    /**
     * §5's first source, and §9's warning attached: undocumented, so any
     * failure here is normal and silent. Open Graph is the real path.
     */
    private fun fetchSubstackPost(url: String): ArticleMetadata? {
        val uri = URI(url)
        val host = uri.host ?: return null
        if (!host.endsWith("substack.com", ignoreCase = true)) return null

        val slug = SLUG.find(uri.path.orEmpty())?.groupValues?.get(1) ?: return null
        val origin = "${uri.scheme}://$host"

        val request = Request.Builder()
            .url("$origin/api/v1/posts/by-slug/$slug")
            .header("User-Agent", USER_AGENT)
            .build()

        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val post = JSONObject(body)

            val bylines = post.optJSONArray("publishedBylines")
            val authors = buildList {
                for (i in 0 until (bylines?.length() ?: 0)) {
                    bylines?.optJSONObject(i)?.optString("name")
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { add(it) }
                }
            }

            return ArticleMetadata(
                title = post.optString("title"),
                coverUrl = post.optString("cover_image"),
                author = authors.joinToString(", "),
            )
        }
    }

    private companion object {
        const val USER_AGENT = "Mozilla/5.0 (compatible; SnipsBot/1.0)"
        const val MAX_HTML_BYTES = 512L * 1024

        val SLUG = Regex("""^/p/([^/?#]+)""")

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }
}
