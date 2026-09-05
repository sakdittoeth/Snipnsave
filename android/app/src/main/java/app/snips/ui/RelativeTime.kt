package app.snips.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * The timestamp beside the publication name, ported from `ago()` in the web
 * prototype so both libraries read the same.
 *
 * Deliberately coarse. A snip saved this morning and one saved at lunch are
 * both "Today"; what the reader is scanning for is roughly how long ago they
 * were reading, not a clock.
 */
fun relativeTime(
    savedAt: Long,
    now: Long = System.currentTimeMillis(),
    locale: Locale = Locale.getDefault(),
): String {
    val days = TimeUnit.MILLISECONDS.toDays((now - savedAt).coerceAtLeast(0))
    return when {
        days < 1 -> "Today"
        days == 1L -> "Yesterday"
        days < 7 -> "${days}d ago"
        days < 35 -> "${days / 7}w ago"
        // Past a month the day itself is more use than the distance.
        else -> SimpleDateFormat("d MMM", locale).format(Date(savedAt))
    }
}
