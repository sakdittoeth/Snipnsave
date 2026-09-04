package app.snips.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareParserTest {

    // Verbatim from a real Chrome "Link to highlight" share, captured with the
    // step 1 intent dump on a phone. Everything about how capture behaves is
    // pinned to this rather than to what §4 predicted.
    private val chromeHighlightShare =
        "\"President Biden will be at Ground Zero in NYC on the 25th anniversary of 9/11 " +
            "with every other living President.\n\nYou know who isn\u2019t going? Donald Trump\u2026 " +
            "because he wasn\u2019t allowed to give a speech. Pathetic.\"\n " +
            "https://substack.com/#:~:text=President%20Biden%20will,a%20speech.%20Pathetic."

    @Test
    fun `chrome sends the whole passage in the text, not just the fragment`() {
        val draft = parseShare(chromeHighlightShare)

        // The fragment holds only "President Biden will" and "a speech. Pathetic."
        // Taking it would silently drop everything in between.
        assertTrue(draft.text, draft.text.startsWith("President Biden will be at Ground Zero"))
        assertTrue(draft.text, draft.text.endsWith("give a speech. Pathetic."))
        assertTrue(draft.text, draft.text.contains("Donald Trump"))
        assertEquals(false, draft.text.contains(ELLIPSIS))
    }

    @Test
    fun `a passage that arrived whole is not marked truncated`() {
        // The link's fragment is a start,end pair, but the text was complete —
        // so the quote can be re-linked from scratch.
        assertEquals(false, parseShare(chromeHighlightShare).fragmentTruncated)
    }

    @Test
    fun `the surrounding quotes chrome adds come off`() {
        val draft = parseShare(chromeHighlightShare)
        assertEquals(false, draft.text.startsWith("\""))
        assertEquals(false, draft.text.endsWith("\""))
    }

    @Test
    fun `the fragment is stripped from the stored url`() {
        assertEquals("https://substack.com/", parseShare(chromeHighlightShare).url)
        assertEquals(UrlSource.SHARED, parseShare(chromeHighlightShare).urlSource)
    }

    @Test
    fun `reading in the substack feed yields a link that goes nowhere useful`() {
        // The real capture above came from substack.com's feed, so the highlight
        // link points at the feed root. Worth warning about in the sheet.
        assertEquals(true, isBareSiteUrl(parseShare(chromeHighlightShare).url))
    }

    @Test
    fun `a real post url is not flagged`() {
        assertEquals(false, isBareSiteUrl("https://pub.substack.com/p/the-post"))
    }

    @Test
    fun `a link-only share still falls back to the fragment for its quote`() {
        val draft = parseShare("https://pub.substack.com/p/the-post#:~:text=the%20selected%20passage")
        assertEquals("the selected passage", draft.text)
        assertEquals("https://pub.substack.com/p/the-post", draft.url)
    }

    @Test
    fun `a link-only start,end share is marked truncated`() {
        val draft = parseShare("https://pub.substack.com/p/the-post#:~:text=first%20six,last%20six")
        assertEquals("first six${ELLIPSIS}last six", draft.text)
        assertEquals(true, draft.fragmentTruncated)
    }

    // ---- the Substack app: Copy the passage, then share the post ----
    //
    // Its reading view offers Copy / Restack quote / Cancel and no overflow,
    // so PROCESS_TEXT never reaches it and a shared post carries no passage.

    @Test
    fun `a bare post link pairs up with the passage on the clipboard`() {
        val draft = parseShare(
            sharedText = "https://pub.substack.com/p/the-post",
            clipboardText = "The passage the reader copied before sharing the post.",
        )
        assertEquals("The passage the reader copied before sharing the post.", draft.text)
        assertEquals("https://pub.substack.com/p/the-post", draft.url)
        assertEquals(TextSource.CLIPBOARD, draft.textSource)
        assertEquals(UrlSource.SHARED, draft.urlSource)
    }

    @Test
    fun `a share that already carries its passage ignores the clipboard`() {
        val draft = parseShare(
            sharedText = "\"The real passage\" https://pub.substack.com/p/the-post",
            clipboardText = "something else entirely",
        )
        assertEquals("The real passage", draft.text)
        assertEquals(TextSource.SHARED, draft.textSource)
    }

    @Test
    fun `a clipboard holding a link is not used as a passage`() {
        val draft = parseShare(
            sharedText = "https://pub.substack.com/p/the-post",
            clipboardText = "https://example.com/something",
        )
        assertEquals("", draft.text)
        assertEquals(TextSource.NONE, draft.textSource)
    }

    @Test
    fun `the clipboard is only consulted when it could add something`() {
        // Bare link — the passage must be on the clipboard.
        assertEquals(true, shareNeedsClipboard("https://pub.substack.com/p/the-post"))
        // Nothing at all.
        assertEquals(true, shareNeedsClipboard(null))
        // Chrome highlight: both halves already present, so no toast is earned.
        assertEquals(false, shareNeedsClipboard(chromeHighlightShare))
        // Passage with no link: the clipboard cannot supply what we have.
        assertEquals(false, shareNeedsClipboard("A passage with no link"))
    }

    @Test
    fun `substack share bundles the link inside the text`() {
        val draft = parseShare("\"A passage worth keeping\" https://pub.substack.com/p/the-post?utm_source=share")
        assertEquals("A passage worth keeping", draft.text)
        assertEquals("https://pub.substack.com/p/the-post", draft.url)
        assertEquals(UrlSource.SHARED, draft.urlSource)
    }

    @Test
    fun `smart quotes come off the passage`() {
        val draft = parseShare("“A passage worth keeping” https://pub.substack.com/p/the-post")
        assertEquals("A passage worth keeping", draft.text)
    }

    @Test
    fun `process_text with no link falls back to the clipboard`() {
        val draft = parseShare(
            sharedText = "A passage with no link attached",
            clipboardText = "https://pub.substack.com/p/the-post",
        )
        assertEquals("A passage with no link attached", draft.text)
        assertEquals("https://pub.substack.com/p/the-post", draft.url)
        assertEquals(UrlSource.CLIPBOARD, draft.urlSource)
    }

    @Test
    fun `the clipboard is ignored when the share already had a link`() {
        val draft = parseShare(
            sharedText = "A passage https://pub.substack.com/p/the-post",
            clipboardText = "https://somewhere.else.com/unrelated",
        )
        assertEquals("https://pub.substack.com/p/the-post", draft.url)
    }

    @Test
    fun `clipboard text that is not a url is never pasted into the snip`() {
        val draft = parseShare(sharedText = "A passage", clipboardText = "my bank password")
        assertEquals("", draft.url)
        assertEquals(UrlSource.NONE, draft.urlSource)
        assertEquals("A passage", draft.text)
    }

    @Test
    fun `a passage with no link anywhere is still a draft worth keeping`() {
        val draft = parseShare("Just the passage, nothing else")
        assertEquals("Just the passage, nothing else", draft.text)
        assertEquals("", draft.url)
        assertEquals(UrlSource.NONE, draft.urlSource)
    }

    @Test
    fun `empty share produces an empty draft rather than throwing`() {
        val draft = parseShare(null)
        assertEquals("", draft.text)
        assertEquals("", draft.url)
    }
}
