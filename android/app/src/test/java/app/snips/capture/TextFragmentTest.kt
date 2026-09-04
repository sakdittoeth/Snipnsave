package app.snips.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TextFragmentTest {

    // ---- reading (§4a) ----

    @Test
    fun `decodes a single-quote fragment`() {
        val parsed = parseTextFragment("https://pub.substack.com/p/x#:~:text=the%20selected%20passage")
        assertEquals("the selected passage", parsed?.quote)
        assertEquals(false, parsed?.truncated)
    }

    @Test
    fun `joins a start,end fragment and flags it truncated`() {
        val parsed = parseTextFragment("https://pub.substack.com/p/x#:~:text=first%20six,last%20six")
        assertEquals("first six${ELLIPSIS}last six", parsed?.quote)
        assertEquals(true, parsed?.truncated)
    }

    @Test
    fun `strips the prefix and suffix context parts`() {
        val parsed = parseTextFragment("https://pub.substack.com/p/x#:~:text=before-,the%20quote,-after")
        assertEquals("the quote", parsed?.quote)
        assertEquals(false, parsed?.truncated)
    }

    @Test
    fun `handles context syntax wrapped around a start,end pair`() {
        val parsed = parseTextFragment("https://pub.substack.com/p/x#:~:text=before-,starts%20here,ends%20here,-after")
        assertEquals("starts here${ELLIPSIS}ends here", parsed?.quote)
        assertEquals(true, parsed?.truncated)
    }

    @Test
    fun `no fragment, nothing to parse`() {
        assertNull(parseTextFragment("https://pub.substack.com/p/x"))
        assertNull(parseTextFragment(null))
    }

    @Test
    fun `plus stays a plus rather than becoming a space`() {
        val parsed = parseTextFragment("https://pub.substack.com/p/x#:~:text=C%2B%2B%20and%20Rust")
        assertEquals("C++ and Rust", parsed?.quote)
    }

    // ---- writing ----

    @Test
    fun `short quote links as one piece`() {
        assertEquals(
            "https://pub.substack.com/p/x#:~:text=a%20short%20passage",
            deepLink("https://pub.substack.com/p/x", "a short passage"),
        )
    }

    @Test
    fun `long quote uses the first and last six words`() {
        val text = "one two three four five six seven eight nine ten eleven twelve thirteen"
        val link = deepLink("https://pub.substack.com/p/x", text)!!
        assertTrue(link, link.contains("text=one%20two%20three%20four%20five%20six,"))
        assertTrue(link, link.endsWith("eight%20nine%20ten%20eleven%20twelve%20thirteen"))
    }

    @Test
    fun `twelve words is still one piece`() {
        val text = "one two three four five six seven eight nine ten eleven twelve"
        assertEquals(false, deepLink("https://pub.substack.com/p/x", text)!!.contains(","))
    }

    @Test
    fun `a truncated quote relinks by its two halves, not by the joined text`() {
        val link = deepLink("https://pub.substack.com/p/x", "first six${ELLIPSIS}last six", fragmentTruncated = true)
        assertEquals("https://pub.substack.com/p/x#:~:text=first%20six,last%20six", link)
    }

    @Test
    fun `a truncated quote whose ellipsis was edited away falls back to the article`() {
        val link = deepLink("https://pub.substack.com/p/x", "someone removed the marker", fragmentTruncated = true)
        assertEquals("https://pub.substack.com/p/x", link)
    }

    @Test
    fun `structural characters are encoded so they are not read as syntax`() {
        val link = deepLink("https://pub.substack.com/p/x", "one-two, three & four")!!
        assertTrue(link, link.contains("%2D"))
        assertTrue(link, link.contains("%2C"))
        assertTrue(link, link.contains("%26"))
    }

    @Test
    fun `no url, no link`() {
        assertNull(deepLink("", "a passage"))
    }

    @Test
    fun `round trips a passage through encode and decode`() {
        val passage = "Curiosity, & the “long-form” essay — C++ included."
        assertEquals(passage, decodeFragmentComponent(encodeFragmentComponent(passage)))
    }
}
