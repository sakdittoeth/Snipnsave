package app.snips.capture

import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

/**
 * Ported from `cleanUrl()` and `fallbackPub()` in the web prototype
 * (HANDOVER §2 lists URL hygiene as carrying over unchanged).
 *
 * Deliberately no `android.*` imports: `android.net.Uri` is a stub in JVM
 * unit tests, and this is exactly the logic worth testing without a device.
 */

/** The tracking params the prototype strips. Same list, same order. */
val JUNK = listOf(
    "utm_source",
    "utm_medium",
    "utm_campaign",
    "utm_content",
    "r",
    "showWelcome",
    "triedRedirect",
    "publication_id",
    "post_id",
)

/**
 * A storable URL: no fragment, no tracking params. The fragment goes because
 * §5 wants the stored URL clean — the text fragment is rebuilt at read time
 * by [deepLink], not carried around in the row.
 *
 * Anything unparseable comes back trimmed rather than empty, matching the
 * prototype: a URL we can't tidy still beats losing it.
 */
fun cleanUrl(raw: String?): String {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isEmpty()) return ""

    // Drop the fragment textually first. A text fragment can contain
    // characters java.net.URI rejects, and we don't want that to cost us
    // the whole cleanup.
    val withoutFragment = trimmed.substringBefore("#")

    return try {
        val uri = URI(withoutFragment)
        if (uri.scheme == null || uri.host == null) return withoutFragment

        val query = uri.rawQuery
            ?.split("&")
            ?.filter { it.isNotEmpty() }
            ?.filterNot { it.substringBefore("=") in JUNK }
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString("&")

        buildString {
            append(uri.scheme).append("://").append(uri.host)
            if (uri.port != -1) append(":").append(uri.port)
            append(uri.rawPath.orEmpty())
            if (query != null) append("?").append(query)
        }
    } catch (_: URISyntaxException) {
        withoutFragment
    }
}

/**
 * The publication name to show until the metadata worker fills in the real
 * one — `hostname` for §5's third metadata source. `foo.substack.com`
 * becomes "Foo"; anything else uses its first label.
 */
fun fallbackPublication(url: String?): String {
    val host = try {
        URI(url?.trim().orEmpty()).host?.removePrefix("www.")
    } catch (_: URISyntaxException) {
        null
    } ?: return "Unknown publication"

    val name = if (host.endsWith(".substack.com")) {
        host.removeSuffix(".substack.com")
    } else {
        host.substringBefore(".")
    }

    return name
        .replace('-', ' ')
        .replace('_', ' ')
        .split(" ")
        .filter { it.isNotEmpty() }
        .joinToString(" ") { word ->
            word.replaceFirstChar { it.titlecase(Locale.getDefault()) }
        }
        .ifEmpty { "Unknown publication" }
}

/** The first http(s) URL sitting in a blob of shared text, if there is one. */
private val URL_IN_TEXT = Regex("""https?://\S+""")

fun findUrl(text: String?): String? = text?.let { URL_IN_TEXT.find(it)?.value }

/** Trailing punctuation a URL picks up when it's pasted mid-sentence. */
fun trimUrlPunctuation(url: String): String = url.trimEnd('.', ',', ')', ']', '"', '\'', '»', '”')
