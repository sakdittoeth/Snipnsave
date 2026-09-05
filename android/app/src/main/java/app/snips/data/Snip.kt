package app.snips.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * HANDOVER.md §5, field for field.
 *
 * Note for step 7: §2 asks that the field names keep the exported JSON
 * compatible with the PWA, but the PWA writes `saved`, `cover` and `logo`
 * where §5 names them `savedAt`, `coverUrl` and `logoUrl`. §5 is the
 * normative spec, so it wins here and the export layer maps the three
 * names on the way out. Nothing else differs.
 */
@Entity(tableName = "snips")
data class Snip(
    @PrimaryKey val id: String,
    val text: String,
    val url: String,
    val note: String = "",
    val publication: String = "",
    val title: String = "",
    val author: String = "",
    val coverUrl: String = "",
    val logoUrl: String = "",
    val savedAt: Long,
    val enriched: Boolean = false,
    /**
     * The quote came from a `start,end` text fragment, so its middle is
     * missing and `deepLink()` must not re-derive a fragment from it as if
     * it were a whole passage. See TextFragment.deepLink.
     */
    val fragmentTruncated: Boolean = false,
)
