package app.snips.capture

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import app.snips.data.Snip
import app.snips.data.SnipDao
import java.util.UUID

/**
 * Holds the draft while the sheet is open, and does the two recovery steps
 * from §4b that need the database or the clipboard.
 */
class CaptureViewModel(private val dao: SnipDao) : ViewModel() {

    var draft by mutableStateOf(CaptureDraft())
        private set

    var note by mutableStateOf("")
        private set

    var saving by mutableStateOf(false)
        private set

    private var loaded = false

    /**
     * @param clipboardText read by the Activity, since only it can reach the
     *   clipboard service — and only when the share carried no link of its own.
     */
    suspend fun load(sharedText: String?, sharedTitle: String?, clipboardText: String?) {
        if (loaded) return
        loaded = true

        var parsed = parseShare(sharedText, sharedTitle, clipboardText)

        // §4b recovery 3: a passage with no link, moments after saving one from
        // an article, is very likely from that same article. Offered, not
        // assumed — UrlSource.RECENT_SNIP makes the sheet say where it came from.
        if (parsed.url.isEmpty()) {
            val recent = dao.mostRecentWithUrlSince(System.currentTimeMillis() - RECENT_WINDOW_MS)
            if (recent != null) {
                parsed = parsed.copy(url = recent.url, urlSource = UrlSource.RECENT_SNIP)
            }
        }

        draft = parsed
    }

    fun onTextChange(value: String) {
        // Editing the passage invalidates the start,end pairing it was
        // recovered with, so the link goes back to being built from scratch.
        draft = draft.copy(text = value, fragmentTruncated = false)
    }

    fun onUrlChange(value: String) {
        draft = draft.copy(url = value, urlSource = UrlSource.SHARED)
    }

    fun onNoteChange(value: String) {
        note = value
    }

    val canSave: Boolean get() = draft.text.isNotBlank() && !saving

    /** @return the new snip's id once it is in the database, or null. */
    suspend fun save(): String? {
        if (!canSave) return null
        saving = true

        val url = cleanUrl(draft.url)
        val id = UUID.randomUUID().toString()
        dao.insert(
            Snip(
                id = id,
                text = draft.text.trim(),
                url = url,
                note = note.trim(),
                title = draft.title,
                publication = if (url.isEmpty()) "" else fallbackPublication(url),
                savedAt = System.currentTimeMillis(),
                enriched = false,
                fragmentTruncated = draft.fragmentTruncated,
            ),
        )
        return id
    }

    private companion object {
        /** "The last few minutes", per §4b. */
        const val RECENT_WINDOW_MS = 5 * 60 * 1000L
    }
}
