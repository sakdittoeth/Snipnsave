package app.snips.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SnipJsonTest {

    private val snip = Snip(
        id = "abc-123",
        text = "Rentiers extract; capitalists invest.",
        url = "https://savageminds.substack.com/p/technofeudalism",
        note = "For the platform rents section.",
        publication = "Savage Minds",
        title = "Technofeudalism and the Future of Capitalism",
        author = "Yanis Varoufakis",
        coverUrl = "https://cdn.example.com/cover.jpg",
        logoUrl = "https://cdn.example.com/logo.png",
        savedAt = 1_757_000_000_000L,
        enriched = true,
        fragmentTruncated = false,
    )

    @Test
    fun `a snip survives a round trip unchanged`() {
        assertEquals(listOf(snip), importSnips(exportSnips(listOf(snip))))
    }

    // ---- §2: the file has to open in the web prototype ----

    @Test
    fun `the export uses the prototype's field names, not the entity's`() {
        val row = JSONObject(exportSnips(listOf(snip))).getJSONArray("snips").getJSONObject(0)
        // §5 renamed these three on the way to Kotlin; the wire format did not.
        assertTrue(row.has("saved"))
        assertTrue(row.has("cover"))
        assertTrue(row.has("logo"))
        assertEquals(false, row.has("savedAt"))
        assertEquals(false, row.has("coverUrl"))
        assertEquals(false, row.has("logoUrl"))
    }

    @Test
    fun `a bare array, as the prototype stores it, imports fine`() {
        val json = """
            [{"id":"x1","text":"A passage","url":"https://example.com/p/one",
              "saved":1757000000000,"cover":"https://example.com/c.jpg","logo":""}]
        """.trimIndent()
        val snips = importSnips(json)
        assertEquals(1, snips.size)
        assertEquals("A passage", snips[0].text)
        assertEquals(1_757_000_000_000L, snips[0].savedAt)
        assertEquals("https://example.com/c.jpg", snips[0].coverUrl)
    }

    @Test
    fun `the entity's own names are read too, for files this app wrote earlier`() {
        val json = """
            {"snips":[{"id":"x2","text":"Another passage","savedAt":1757000000000,
                       "coverUrl":"https://example.com/d.jpg"}]}
        """.trimIndent()
        val snips = importSnips(json)
        assertEquals(1_757_000_000_000L, snips[0].savedAt)
        assertEquals("https://example.com/d.jpg", snips[0].coverUrl)
    }

    // ---- import is forgiving, but not careless ----

    @Test
    fun `a row with no passage is skipped rather than imported empty`() {
        val json = """{"snips":[{"id":"a","text":""},{"id":"b","text":"Real passage"}]}"""
        val snips = importSnips(json)
        assertEquals(1, snips.size)
        assertEquals("Real passage", snips[0].text)
    }

    @Test
    fun `missing fields fall back rather than failing the whole file`() {
        val snips = importSnips("""{"snips":[{"text":"Bare minimum"}]}""")
        assertEquals(1, snips.size)
        assertEquals("", snips[0].url)
        assertEquals(false, snips[0].enriched)
        assertTrue(snips[0].id.isNotEmpty())
    }

    @Test
    fun `rows without ids get distinct ones so they cannot collide`() {
        val snips = importSnips("""{"snips":[{"text":"One"},{"text":"Two"}]}""")
        assertEquals(2, snips.map { it.id }.distinct().size)
    }

    @Test
    fun `an empty library exports and re-imports as empty`() {
        assertEquals(emptyList<Snip>(), importSnips(exportSnips(emptyList())))
    }

    @Test
    fun `a file with no snips key yields nothing rather than throwing`() {
        assertEquals(emptyList<Snip>(), importSnips("""{"format":"snips.v1"}"""))
    }

    @Test(expected = org.json.JSONException::class)
    fun `something that is not json at all is rejected`() {
        importSnips("this is not a backup")
    }

    @Test
    fun `the export is stamped so a reader can tell what it is`() {
        val doc = JSONObject(exportSnips(listOf(snip)))
        assertEquals("snips.v1", doc.getString("format"))
        assertTrue(doc.getLong("exportedAt") > 0L)
    }

    @Test
    fun `passages with quotes and newlines survive the trip`() {
        val awkward = snip.copy(
            id = "awkward",
            text = "She said \"no\" — twice.\n\nThen left.",
            note = "Contains \\ backslashes / slashes",
        )
        val back = importSnips(exportSnips(listOf(awkward))).single()
        assertEquals(awkward.text, back.text)
        assertEquals(awkward.note, back.note)
    }
}
