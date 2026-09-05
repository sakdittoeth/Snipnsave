package app.snips.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleMetadataTest {

    private val substackHtml = """
        <html><head>
          <title>Technofeudalism and the Future of Capitalism - Savage Minds</title>
          <meta property="og:site_name" content="Savage Minds">
          <meta property="og:title" content="Technofeudalism and the Future of Capitalism - Savage Minds">
          <meta property="og:image" content="https://substackcdn.com/image/fetch/cover.jpg">
          <meta name="author" content="Cory Doctorow">
          <link rel="apple-touch-icon" sizes="180x180" href="/icon-180.png">
          <link rel="icon" sizes="16x16" href="/favicon.ico">
        </head><body><article>Body text we must never store.</article></body></html>
    """.trimIndent()

    private val url = "https://savageminds.substack.com/p/technofeudalism-and-the-future-of"

    @Test
    fun `reads the open graph fields`() {
        val meta = parseHtmlMetadata(substackHtml, url)
        assertEquals("Savage Minds", meta.publication)
        assertEquals("Cory Doctorow", meta.author)
        assertEquals("https://substackcdn.com/image/fetch/cover.jpg", meta.coverUrl)
    }

    @Test
    fun `strips the publication suffix substack appends to titles`() {
        // "… - Savage Minds" is already on the card's attribution line.
        assertEquals(
            "Technofeudalism and the Future of Capitalism",
            parseHtmlMetadata(substackHtml, url).title,
        )
    }

    @Test
    fun `prefers the apple touch icon over a tiny favicon`() {
        assertEquals(
            "https://savageminds.substack.com/icon-180.png",
            parseHtmlMetadata(substackHtml, url).logoUrl,
        )
    }

    @Test
    fun `a by-slug seed wins over the page's own tags`() {
        val seed = ArticleMetadata(title = "The Real Title", author = "A. Byline")
        val meta = parseHtmlMetadata(substackHtml, url, seed)
        assertEquals("The Real Title", meta.title)
        assertEquals("A. Byline", meta.author)
        // …but the seed carries no site name, so Open Graph still supplies it.
        assertEquals("Savage Minds", meta.publication)
    }

    // ---- any site, not just Substack ----

    @Test
    fun `an ordinary news article parses the same way`() {
        val html = """
            <html><head>
              <title>Why Cities Stopped Building</title>
              <meta property="og:site_name" content="The Atlantic">
              <meta property="og:title" content="Why Cities Stopped Building">
              <meta property="og:image" content="https://cdn.theatlantic.com/lead.jpg">
              <meta property="article:author" content="Jane Reporter">
              <link rel="icon" href="https://cdn.theatlantic.com/favicon.png">
            </head><body></body></html>
        """.trimIndent()
        val meta = parseHtmlMetadata(html, "https://www.theatlantic.com/ideas/archive/2026/01/cities/")
        assertEquals("The Atlantic", meta.publication)
        assertEquals("Why Cities Stopped Building", meta.title)
        assertEquals("Jane Reporter", meta.author)
        assertEquals("https://cdn.theatlantic.com/favicon.png", meta.logoUrl)
    }

    @Test
    fun `twitter tags stand in when open graph is missing`() {
        val html = """
            <html><head>
              <meta name="twitter:title" content="A Post Without Open Graph">
              <meta name="twitter:image" content="/media/card.png">
            </head><body></body></html>
        """.trimIndent()
        val meta = parseHtmlMetadata(html, "https://example.com/writing/post")
        assertEquals("A Post Without Open Graph", meta.title)
        assertEquals("https://example.com/media/card.png", meta.coverUrl)
    }

    @Test
    fun `a page with no metadata at all still names its publication`() {
        val meta = parseHtmlMetadata("<html><head></head><body>nothing</body></html>", "https://example.com/x")
        assertEquals("Example", meta.publication)
        assertEquals("", meta.title)
        assertEquals("", meta.coverUrl)
    }

    @Test
    fun `the document title stands in when no meta tags carry one`() {
        val html = "<html><head><title>Plain Old Title</title></head><body></body></html>"
        assertEquals("Plain Old Title", parseHtmlMetadata(html, "https://example.com/x").title)
    }

    @Test
    fun `relative image paths are resolved against the page`() {
        val html = """<html><head><meta property="og:image" content="../img/cover.jpg"></head></html>"""
        assertEquals(
            "https://example.com/img/cover.jpg",
            parseHtmlMetadata(html, "https://example.com/posts/one").coverUrl,
        )
    }

    @Test
    fun `no article body is ever carried out of the parse`() {
        // §9: only user-selected passages are stored, never article content.
        val meta = parseHtmlMetadata(substackHtml, url)
        val everything = listOf(meta.publication, meta.title, meta.author, meta.coverUrl, meta.logoUrl)
        assertTrue(everything.none { it.contains("Body text") })
    }

    @Test
    fun `malformed html does not throw`() {
        val meta = parseHtmlMetadata("<html><head><meta property=og:title content=Broken", "https://example.com/x")
        assertEquals("Example", meta.publication)
    }
}
