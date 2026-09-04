package app.snips.capture

/**
 * Text fragments, both directions.
 *
 * Reading them is §4a: Chrome's "Link to highlight" hands us a URL that
 * already carries the passage, so we decode the quote back out of it and
 * get a verified-working deep link for free.
 *
 * Writing them is `deepLink()` from the prototype: a passage over twelve
 * words is linked by its first and last six, so the link survives small
 * edits to the post.
 *
 * https://developer.mozilla.org/en-US/docs/Web/URI/Fragment/Text_fragments
 */

const val FRAGMENT_MARKER = "#:~:"

/** What separates the two halves of a quote recovered from a `start,end` link. */
const val ELLIPSIS = " … "

data class ParsedFragment(
    /** The passage, with `start,end` halves joined by [ELLIPSIS]. */
    val quote: String,
    /** True when the quote is a `start,end` pair and its middle is missing. */
    val truncated: Boolean,
)

/**
 * Pull the passage out of a text-fragment URL.
 *
 * Handles all three shapes §4a lists: `text=quote`, `text=start,end`, and the
 * `prefix-,quote,-suffix` context syntax, whose `-`-delimited parts are
 * context for the browser's matcher rather than part of the quote.
 */
fun parseTextFragment(url: String?): ParsedFragment? {
    val fragment = url?.substringAfter(FRAGMENT_MARKER, "").orEmpty()
    if (fragment.isEmpty()) return null

    // A fragment directive can carry several `&`-separated parts; the passage
    // is the first `text=` among them.
    val directive = fragment.split("&").firstOrNull { it.startsWith("text=") } ?: return null

    var parts = directive.removePrefix("text=").split(",").filter { it.isNotEmpty() }
    if (parts.isEmpty()) return null

    // prefix- and -suffix bracket the quote and aren't part of it.
    if (parts.size > 1 && parts.first().endsWith("-")) parts = parts.drop(1)
    if (parts.size > 1 && parts.last().startsWith("-")) parts = parts.dropLast(1)

    val decoded = parts.map(::decodeFragmentComponent).filter { it.isNotBlank() }
    return when (decoded.size) {
        0 -> null
        1 -> ParsedFragment(decoded[0], truncated = false)
        // start,end — everything between the two is missing.
        else -> ParsedFragment(decoded.first() + ELLIPSIS + decoded.last(), truncated = true)
    }
}

/**
 * A URL that opens the article at the passage.
 *
 * A quote we only hold the ends of is linked by those ends, exactly as it
 * arrived — rebuilding a fragment from the joined halves would search the
 * post for an ellipsis that isn't in it.
 */
fun deepLink(url: String, text: String, fragmentTruncated: Boolean = false): String? {
    if (url.isBlank()) return null

    // Canonicalise again here, not only at capture. A fragment is dropped
    // across Substack's open.substack.com redirect, so a row saved before
    // cleanUrl started rewriting them would otherwise land at the top of the
    // post forever. Cheap, and it repairs old rows on the way out.
    val target = canonicalise(url)

    val passage = text.trim()
    if (passage.isEmpty()) return target

    if (fragmentTruncated) {
        val halves = passage.split(ELLIPSIS)
        if (halves.size == 2) {
            return target + FRAGMENT_MARKER + "text=" +
                encodeFragmentComponent(halves[0].trim()) + "," +
                encodeFragmentComponent(halves[1].trim())
        }
        // The ellipsis is gone — most likely the passage was edited. Falling
        // back to the plain article beats linking to a phrase that isn't there.
        return target
    }

    val words = passage.split(WHITESPACE).filter { it.isNotEmpty() }
    if (words.size <= 12) {
        return target + FRAGMENT_MARKER + "text=" + encodeFragmentComponent(passage)
    }

    val start = words.take(6).joinToString(" ")
    val end = words.takeLast(6).joinToString(" ")
    return target + FRAGMENT_MARKER + "text=" +
        encodeFragmentComponent(start) + "," + encodeFragmentComponent(end)
}

private val WHITESPACE = Regex("""\s+""")

/**
 * `encodeURIComponent`, then the three characters a text fragment treats as
 * structure — `-` brackets context, `,` separates parts, `&` separates
 * directives — as in the prototype.
 *
 * java.net.URLEncoder can't stand in: it encodes `~ ! ' ( ) *` and writes a
 * space as `+`, which a fragment matcher reads literally.
 */
fun encodeFragmentComponent(value: String): String {
    val out = StringBuilder()
    for (byte in value.toByteArray(Charsets.UTF_8)) {
        val char = (byte.toInt() and 0xFF).toChar()
        if (char in UNRESERVED) out.append(char) else out.append('%').append(HEX[byte.toInt() and 0xFF])
    }
    return out.toString()
        .replace("-", "%2D")
        .replace(",", "%2C")
        .replace("&", "%26")
}

/**
 * Percent-decoding only. `+` stays a plus sign — fragments are percent-encoded,
 * and reading `+` as a space would silently corrupt any quote containing one.
 */
fun decodeFragmentComponent(value: String): String {
    val bytes = ArrayList<Byte>(value.length)
    var i = 0
    while (i < value.length) {
        val char = value[i]
        if (char == '%' && i + 2 < value.length) {
            val hex = value.substring(i + 1, i + 3).toIntOrNull(16)
            if (hex != null) {
                bytes.add(hex.toByte())
                i += 3
                continue
            }
        }
        for (b in char.toString().toByteArray(Charsets.UTF_8)) bytes.add(b)
        i++
    }
    return String(bytes.toByteArray(), Charsets.UTF_8)
}

private const val UNRESERVED =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.!~*'()"

private val HEX = Array(256) { "%02X".format(it) }
