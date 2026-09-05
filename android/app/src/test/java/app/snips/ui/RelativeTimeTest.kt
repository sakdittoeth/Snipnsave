package app.snips.ui

import java.util.Locale
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeTimeTest {

    private val now = 1_757_000_000_000L // a fixed instant, so the tests don't drift

    private fun daysAgo(days: Long) = now - TimeUnit.DAYS.toMillis(days)

    @Test
    fun `today`() {
        assertEquals("Today", relativeTime(now, now))
        assertEquals("Today", relativeTime(now - TimeUnit.HOURS.toMillis(23), now))
    }

    @Test
    fun `yesterday`() {
        assertEquals("Yesterday", relativeTime(daysAgo(1), now))
    }

    @Test
    fun `days within the week`() {
        assertEquals("3d ago", relativeTime(daysAgo(3), now))
        assertEquals("6d ago", relativeTime(daysAgo(6), now))
    }

    @Test
    fun `weeks up to about a month`() {
        assertEquals("1w ago", relativeTime(daysAgo(7), now))
        assertEquals("4w ago", relativeTime(daysAgo(34), now))
    }

    @Test
    fun `past a month it shows the date instead`() {
        val label = relativeTime(daysAgo(60), now, Locale.UK)
        assertEquals(false, label.contains("ago"))
        assertEquals(true, label.first().isDigit())
    }

    @Test
    fun `a clock skewed into the future still reads as today`() {
        assertEquals("Today", relativeTime(now + TimeUnit.HOURS.toMillis(5), now))
    }
}
