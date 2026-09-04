package app.snips.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.snips.R

/**
 * §6: Spectral at 18sp/1.6 for passages, system sans for all chrome.
 *
 * Google Fonts publishes Spectral as static weights rather than the variable
 * font §6 assumed, so the three faces in use are bundled individually.
 */
val PassageFontFamily = FontFamily(
    Font(R.font.spectral_regular, FontWeight.Normal),
    Font(R.font.spectral_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.spectral_semibold, FontWeight.SemiBold),
)

/** The passage itself — the hero of every card. */
val PassageStyle = TextStyle(
    fontFamily = PassageFontFamily,
    fontSize = 18.sp,
    lineHeight = 28.8.sp, // 18 × 1.6
)

/** The reader's own note: same serif, italic, and muted at the call site. */
val NoteStyle = PassageStyle.copy(
    fontSize = 16.sp,
    lineHeight = 25.6.sp,
    fontStyle = FontStyle.Italic,
)

private val Sans = FontFamily.SansSerif

val SnipTypography = Typography(
    titleMedium = TextStyle(fontFamily = Sans, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontSize = 12.sp, lineHeight = 16.sp),
)
