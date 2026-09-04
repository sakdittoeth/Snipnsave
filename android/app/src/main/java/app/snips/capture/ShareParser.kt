package app.snips.capture

import java.net.URI
import java.net.URISyntaxException

/**
 * Turns whatever a share intent carried into a draft snip.
 *
 * Pure on purpose: an Intent is unpleasant to construct in a test, and the
 * decisions worth testing are all about the strings inside it. The Activity
 * pulls the extras out; this decides what they mean.
 */

/** Where the article URL came from, so the sheet can say so. §4b's recovery ladder. */
enum class UrlSource {
    /** It was in the shared text — the ordinary case. */
    SHARED,
    /** Recovered from the clipboard because the share carried no link. */
    CLIPBOARD,
    /** Borrowed from a snip saved from the same reading session, moments ago. */
    RECENT_SNIP,
    /** No link anywhere. A snip without one is still worth keeping. */
    NONE,
}

/** Where the passage came from. */
enum class TextSource {
    /** It was in the share itself. */
    SHARED,
    /**
     * Recovered from the clipboard because the share was a bare link.
     *
     * This is the Substack app's only route in. Its reading view replaces the
     * system text-selection toolbar with Copy / Restack quote / Cancel and
     * offers no overflow, so PROCESS_TEXT never appears there and a shared
     * post carries no passage. Copy the passage, share the post, and the two
     * halves meet here.
     */
    CLIPBOARD,
    /** Nothing to show — an empty sheet for the reader to fill in. */
    NONE,
}

data class CaptureDraft(
    val text: String = "",
    val url: String = "",
    val urlSource: UrlSource = UrlSource.NONE,
    val textSource: TextSource = TextSource.NONE,
    val fragmentTruncated: Boolean = false,
)

/**
 * Whether reading the clipboard could add anything to this share.
 *
 * Worth asking before doing it: from Android 12 a clipboard read raises a
 * system toast, and there's no reason to earn one when the share already
 * carries both halves.
 */
fun shareNeedsClipboard(sharedText: String?): Boolean {
    val shared = sharedText.orEmpty().trim()
    if (shared.isEmpty()) return true
    val url = findUrl(shared)?.let(::trimUrlPunctuation)
        ?: return false // Text but no link: the clipboard can't supply a passage we already have.
    // A bare link — the passage, if there is one, is on the clipboard.
    return parseTextFragment(url) == null && stripQuotes(shared.replace(url, "").trim()).isEmpty()
}

/**
 * @param sharedText EXTRA_TEXT, or EXTRA_PROCESS_TEXT for §4b.
 * @param clipboardText checked only when the share carried no URL of its own.
 *
 * EXTRA_SUBJECT is deliberately not an input: it carries the post's title,
 * and a title is not a passage. It becomes the title fallback in step 5,
 * where metadata is the subject.
 */
fun parseShare(
    sharedText: String?,
    clipboardText: String? = null,
): CaptureDraft {
    val shared = sharedText.orEmpty().trim()

    val sharedUrl = findUrl(shared)?.let(::trimUrlPunctuation)
    val fragment = parseTextFragment(sharedUrl)

    // What's left of the share once the URL is lifted out of it.
    val remainder = if (sharedUrl != null) {
        stripQuotes(shared.replace(sharedUrl, "").trim())
    } else {
        stripQuotes(shared)
    }

    // §4a assumed the fragment was the best source for the quote. A real
    // Chrome "Link to highlight" share says otherwise: EXTRA_TEXT carries the
    // *whole* selected passage, while the fragment holds only the first and
    // last few words of it. The page title Chrome sends goes in EXTRA_SUBJECT
    // and EXTRA_TITLE, not here, so the remainder is the passage — and taking
    // the fragment instead would throw away the middle of what was selected.
    val sharedQuote = if (remainder.isNotEmpty()) remainder else fragment?.quote.orEmpty()

    // The Substack app's route in: it shares a bare post link, and the passage
    // is on the clipboard because Copy was the only thing its toolbar offered.
    // A clipboard that holds a link rather than prose is somebody's URL, not a
    // passage, so it is left for the URL recovery below instead.
    val clipboardQuote = if (sharedQuote.isEmpty() && findUrl(clipboardText) == null) {
        stripQuotes(clipboardText.orEmpty().trim())
    } else {
        ""
    }

    val quote = sharedQuote.ifEmpty { clipboardQuote }

    // Only a quote we actually recovered from a start,end fragment is missing
    // its middle. When the full passage came through the text, it isn't.
    val truncated = fragment?.truncated == true && remainder.isEmpty()

    // §4b recovery 2. Only ever a URL: pasting arbitrary clipboard contents
    // in as a *link* would be worse than leaving the field empty.
    val clipboardUrl = if (sharedUrl == null) {
        findUrl(clipboardText)?.let(::trimUrlPunctuation)
    } else {
        null
    }

    val url = sharedUrl ?: clipboardUrl
    return CaptureDraft(
        text = quote,
        url = cleanUrl(url),
        urlSource = when {
            sharedUrl != null -> UrlSource.SHARED
            clipboardUrl != null -> UrlSource.CLIPBOARD
            else -> UrlSource.NONE
        },
        textSource = when {
            sharedQuote.isNotEmpty() -> TextSource.SHARED
            clipboardQuote.isNotEmpty() -> TextSource.CLIPBOARD
            else -> TextSource.NONE
        },
        fragmentTruncated = truncated,
    )
}

/**
 * True when a URL points at a site's front door rather than at something to
 * read — `https://substack.com/`, `https://pub.substack.com/`.
 *
 * This matters more than it looks. Reading inside substack.com's own feed and
 * sharing from there produces a highlight link to the feed root, so "read in
 * context" has nothing to return to: the passage isn't at that address, and
 * won't be there tomorrow either. The sheet warns rather than refuses — the
 * snip is still worth keeping — but it's the difference between the app's two
 * promises and only one of them.
 */
fun isBareSiteUrl(url: String): Boolean {
    if (url.isBlank()) return false
    val path = try {
        URI(url).path
    } catch (_: URISyntaxException) {
        return false
    }
    return path.isNullOrEmpty() || path == "/"
}

/** Substack's share wraps the passage in quotes; they aren't part of it. */
internal fun stripQuotes(text: String): String =
    text.trim().removeSurrounding("\"")
        .removeSurrounding("“", "”")
        .removeSurrounding("‘", "’")
        .trim()
