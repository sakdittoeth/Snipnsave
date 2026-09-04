package app.snips.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * §6: Spectral at 18sp/1.6 for passages, system sans for all chrome.
 *
 * TODO(step 3): drop the Spectral variable font into res/font and swap
 * FontFamily.Serif for it. Serif keeps the passage visually distinct from
 * the chrome in the meantime, which is the property that matters most.
 */
val PassageFontFamily = FontFamily.Serif

/** The passage itself — the hero of every card. */
val PassageStyle = TextStyle(
    fontFamily = PassageFontFamily,
    fontSize = 18.sp,
    lineHeight = 28.8.sp, // 18 * 1.6
)

/** The user's own note: same serif, italic, muted at the call site. */
val NoteStyle = PassageStyle.copy(fontSize = 16.sp, lineHeight = 25.6.sp)

private val Sans = FontFamily.SansSerif

val SnipTypography = Typography(
    titleMedium = TextStyle(fontFamily = Sans, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontSize = 12.sp, lineHeight = 16.sp),
)
