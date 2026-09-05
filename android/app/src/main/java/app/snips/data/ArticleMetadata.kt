package app.snips.data

import app.snips.capture.fallbackPublication
import java.net.URI
import java.net.URISyntaxException
import org.jsoup.Jsoup

/**
 * The five fields a card needs, and the parse that finds them.
 *
 * Ported from `metadata-worker.js` in the web prototype, which §5 names as a
 * working reference for exactly this. The worker itself is dropped — §2 is
 * explicit that it only existed to dodge browser CORS, and a native app
 * fetches the page directly.
 *
 * Open Graph is the path that matters. It is also what makes this work for
 * any article rather than Substack alone: nothing here is Substack-specific
 * except the optional `by-slug` seed, which §9 says must never be
 * load-bearing.
 *
 * Deliberately no `android.*` imports — Jsoup is a plain JVM library, so the
 * parse is testable without a device.
 */
data class ArticleMetadata(
    val publication: String = "",
    val title: String = "",
    val author: String = "",
    val coverUrl: String = "",
    val logoUrl: String = "",
) {
    val isEmpty: Boolean
        get() = publication.isEmpty() && title.isEmpty() && author.isEmpty() &&
            coverUrl.isEmpty() && logoUrl.isEmpty()
}

/**
 * @param seed anything already known — in practice the `by-slug` result,
 *   which wins over Open Graph because it is cleaner when it is there at all.
 */
fun parseHtmlMetadata(
    html: String,
    pageUrl: String,
    seed: ArticleMetadata = ArticleMetadata(),
): ArticleMetadata {
    // Jsoup resolves relative hrefs against the base URI, which is how a
    // favicon written as "/icon.png" becomes something we can fetch.
    val document = Jsoup.parse(html, pageUrl)

    // Both `property` (Open Graph) and `name` (nearly everything else) are
    // used in the wild, often on the same page.
    val meta = buildMap {
        for (tag in document.select("meta")) {
            val key = tag.attr("property").ifEmpty { tag.attr("name") }.lowercase()
            val value = tag.attr("content")
            if (key.isNotEmpty() && value.isNotEmpty() && !containsKey(key)) put(key, value)
        }
    }

    val publication = meta["og:site_name"].orEmpty()
        .ifEmpty { seed.publication }
        .ifEmpty { fallbackPublication(pageUrl) }

    var title = seed.title
        .ifEmpty { meta["og:title"].orEmpty() }
        .ifEmpty { meta["twitter:title"].orEmpty() }
        .ifEmpty { document.title() }

    // Substack titles arrive as "Post title - Publication". The publication is
    // already on the card's attribution line; repeating it in the title just
    // costs a line of space.
    val suffix = " - $publication"
    if (publication.isNotEmpty() && title.endsWith(suffix)) {
        title = title.dropLast(suffix.length)
    }

    return ArticleMetadata(
        publication = publication,
        title = title.trim(),
        author = seed.author
            .ifEmpty { meta["author"].orEmpty() }
            .ifEmpty { meta["article:author"].orEmpty() }
            .trim(),
        coverUrl = seed.coverUrl
            .ifEmpty { meta["og:image"].orEmpty() }
            .ifEmpty { meta["twitter:image"].orEmpty() }
            .let { absolute(it, pageUrl) },
        logoUrl = absolute(pickIcon(document), pageUrl),
    )
}

/**
 * The best site icon on the page. Apple's touch icon is usually the real
 * logo rather than a 16px favicon, and a declared size of 180 or 192 says
 * the same thing — so those win over whatever came first.
 */
private fun pickIcon(document: org.jsoup.nodes.Document): String {
    var best = ""
    for (link in document.select("link[rel~=(?i)icon]")) {
        val href = link.attr("abs:href").ifEmpty { link.attr("href") }
        if (href.isEmpty()) continue
        val preferred = link.attr("rel").contains("apple-touch", ignoreCase = true) ||
            link.attr("sizes").let { it.contains("180") || it.contains("192") }
        if (best.isEmpty() || preferred) best = href
        if (preferred) break
    }
    return best
}

private fun absolute(url: String, pageUrl: String): String {
    if (url.isEmpty()) return ""
    return try {
        URI(pageUrl).resolve(url).toString()
    } catch (_: URISyntaxException) {
        url
    } catch (_: IllegalArgumentException) {
        url
    }
}
