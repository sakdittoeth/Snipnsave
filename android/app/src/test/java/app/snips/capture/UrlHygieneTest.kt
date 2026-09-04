package app.snips.capture

import org.junit.Assert.assertEquals
import org.junit.Test

class UrlHygieneTest {

    @Test
    fun `strips tracking params`() {
        assertEquals(
            "https://pub.substack.com/p/the-post",
            cleanUrl("https://pub.substack.com/p/the-post?utm_source=twitter&r=abc123&post_id=99"),
        )
    }

    @Test
    fun `keeps params that are not junk`() {
        assertEquals(
            "https://pub.substack.com/p/the-post?comments=true",
            cleanUrl("https://pub.substack.com/p/the-post?utm_medium=email&comments=true"),
        )
    }

    @Test
    fun `drops the fragment`() {
        assertEquals(
            "https://pub.substack.com/p/the-post",
            cleanUrl("https://pub.substack.com/p/the-post#:~:text=some%20passage"),
        )
    }

    @Test
    fun `leaves a trailing question mark off when every param was junk`() {
        assertEquals(
            "https://pub.substack.com/p/the-post",
            cleanUrl("https://pub.substack.com/p/the-post?utm_source=x"),
        )
    }

    @Test
    fun `passes through something it cannot parse rather than losing it`() {
        assertEquals("not a url at all", cleanUrl("  not a url at all  "))
    }

    @Test
    fun `empty in, empty out`() {
        assertEquals("", cleanUrl(null))
        assertEquals("", cleanUrl("   "))
    }

    @Test
    fun `substack subdomain becomes the publication name`() {
        assertEquals("Astral Codex Ten", fallbackPublication("https://astral-codex-ten.substack.com/p/x"))
    }

    @Test
    fun `non-substack host falls back to its first label`() {
        assertEquals("Example", fallbackPublication("https://www.example.com/article"))
    }

    @Test
    fun `unknown when there is no host`() {
        assertEquals("Unknown publication", fallbackPublication(""))
    }

    @Test
    fun `finds a url inside shared text`() {
        assertEquals(
            "https://pub.substack.com/p/x",
            findUrl("Great line this — https://pub.substack.com/p/x"),
        )
    }

    @Test
    fun `a full stop that belongs to a text fragment is left alone`() {
        val url = "https://substack.com/#:~:text=President%20Biden%20will,a%20speech.%20Pathetic."
        assertEquals(url, trimUrlPunctuation(url))
    }

    @Test
    fun `trims punctuation a url picked up from prose`() {
        assertEquals(
            "https://pub.substack.com/p/x",
            trimUrlPunctuation("https://pub.substack.com/p/x)."),
        )
    }
}
