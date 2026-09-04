package app.snips.capture

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

data class CaptureDraft(
    val text: String = "",
    val url: String = "",
    val urlSource: UrlSource = UrlSource.NONE,
    val fragmentTruncated: Boolean = false,
)

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

    val quote = when {
        // §4a: the passage is already inside the link, and it's the passage the
        // browser verified it can scroll to. Prefer it over anything else in the
        // share — Chrome puts the page title there, not the selection.
        fragment != null -> fragment.quote
        else -> stripQuotes(shared.replace(sharedUrl.orEmpty(), "").trim())
    }

    // §4b recovery 2. Only ever a URL: pasting arbitrary clipboard contents
    // into someone's snip would be worse than leaving the field empty.
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
        fragmentTruncated = fragment?.truncated == true,
    )
}

/** Substack's share wraps the passage in quotes; they aren't part of it. */
internal fun stripQuotes(text: String): String =
    text.trim().removeSurrounding("\"")
        .removeSurrounding("“", "”")
        .removeSurrounding("‘", "’")
        .trim()
