package app.snips.capture

import org.junit.Assert.assertEquals
import org.junit.Test

class ShareParserTest {

    @Test
    fun `chrome highlight link gives us the quote and the article`() {
        val draft = parseShare("https://pub.substack.com/p/the-post#:~:text=the%20selected%20passage")
        assertEquals("the selected passage", draft.text)
        assertEquals("https://pub.substack.com/p/the-post", draft.url)
        assertEquals(UrlSource.SHARED, draft.urlSource)
        assertEquals(false, draft.fragmentTruncated)
    }

    @Test
    fun `a start,end highlight link is marked truncated`() {
        val draft = parseShare("https://pub.substack.com/p/the-post#:~:text=first%20six,last%20six")
        assertEquals("first six${ELLIPSIS}last six", draft.text)
        assertEquals(true, draft.fragmentTruncated)
    }

    @Test
    fun `a page title riding along with a highlight link is not mistaken for the quote`() {
        val draft = parseShare(
            sharedText = "The Post Everyone Is Talking About\nhttps://pub.substack.com/p/the-post#:~:text=the%20real%20passage",
        )
        assertEquals("the real passage", draft.text)
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
