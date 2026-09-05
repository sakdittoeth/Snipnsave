package app.snips.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The palette from HANDOVER.md §6, carried over from the web prototype.
 *
 * These are not Material roles. Material's colour system wants to spread an
 * accent across a dozen slots; this design puts orange in exactly two places
 * and everything else on paper. Keeping our own token set is what stops
 * Material from doing that spreading for us.
 */
@Immutable
data class SnipColors(
    val paper: Color,
    val tint: Color,
    val ink: Color,
    val muted: Color,
    val hair: Color,
    val mark: Color,
)

/** Substack orange. The highlight rule and the primary button — nothing else. */
private val Mark = Color(0xFFFF6719)

val LightColors = SnipColors(
    paper = Color(0xFFFFFFFF),
    tint = Color(0xFFFAF9F8),
    ink = Color(0xFF1A1A1A),
    muted = Color(0xFF6B6B6B),
    hair = Color(0xFFE7E5E4),
    mark = Mark,
)

val DarkColors = SnipColors(
    paper = Color(0xFF111110),
    tint = Color(0xFF1C1B1A),
    ink = Color(0xFFF0EFED),
    muted = Color(0xFF9A9793),
    hair = Color(0xFF2E2C2A),
    mark = Mark,
)
