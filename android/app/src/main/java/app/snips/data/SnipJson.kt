package app.snips.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * The export format, and the reason it isn't simply the Kotlin model.
 *
 * §2 asks that the exported JSON stay compatible with the web prototype, but
 * §5 renamed three fields on the way to Kotlin: the prototype writes `saved`,
 * `cover` and `logo` where the entity has `savedAt`, `coverUrl` and `logoUrl`.
 * Both can be honoured — the wire format keeps the prototype's names, and this
 * file is the only place that knows about the difference.
 *
 * Import is forgiving in the other direction: it reads either name, so a file
 * written by an older build of this app opens too.
 */
private const val FORMAT = "snips.v1"

fun exportSnips(snips: List<Snip>): String {
    val rows = JSONArray()
    for (snip in snips) {
        rows.put(
            JSONObject().apply {
                put("id", snip.id)
                put("text", snip.text)
                put("url", snip.url)
                put("note", snip.note)
                put("publication", snip.publication)
                put("title", snip.title)
                put("author", snip.author)
                // The prototype's names, deliberately.
                put("cover", snip.coverUrl)
                put("logo", snip.logoUrl)
                put("saved", snip.savedAt)
                put("enriched", snip.enriched)
                put("fragmentTruncated", snip.fragmentTruncated)
            },
        )
    }

    return JSONObject().apply {
        put("format", FORMAT)
        put("exportedAt", System.currentTimeMillis())
        put("snips", rows)
    }.toString(2)
}

/**
 * Reads either a wrapped export or a bare array — the prototype's localStorage
 * holds the array on its own, and someone will paste that in eventually.
 *
 * A row without a passage is skipped rather than imported empty; everything
 * else has a sane default, because a partial snip still beats losing it.
 *
 * @throws org.json.JSONException if the text isn't JSON at all.
 */
fun importSnips(json: String): List<Snip> {
    val trimmed = json.trim()
    val rows = if (trimmed.startsWith("[")) {
        JSONArray(trimmed)
    } else {
        JSONObject(trimmed).optJSONArray("snips") ?: JSONArray()
    }

    val snips = ArrayList<Snip>(rows.length())
    for (i in 0 until rows.length()) {
        val row = rows.optJSONObject(i) ?: continue
        val text = row.optString("text").trim()
        if (text.isEmpty()) continue

        snips.add(
            Snip(
                // A row without an id would collide with every other one, so
                // it gets a stable id derived from what it does carry.
                id = row.optString("id").ifEmpty { "imported-${row.optLong("saved", 0L)}-$i" },
                text = text,
                url = row.optString("url"),
                note = row.optString("note"),
                publication = row.optString("publication"),
                title = row.optString("title"),
                author = row.optString("author"),
                coverUrl = row.optString("cover").ifEmpty { row.optString("coverUrl") },
                logoUrl = row.optString("logo").ifEmpty { row.optString("logoUrl") },
                savedAt = row.optLong("saved", 0L)
                    .takeIf { it > 0L }
                    ?: row.optLong("savedAt", System.currentTimeMillis()),
                enriched = row.optBoolean("enriched", false),
                fragmentTruncated = row.optBoolean("fragmentTruncated", false),
            ),
        )
    }
    return snips
}
