package app.snips.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalSnipColors = staticCompositionLocalOf { LightColors }

/**
 * Reach the §6 tokens from anywhere in the tree: `SnipTheme.colors.mark`.
 */
object SnipTheme {
    val colors: SnipColors
        @Composable @ReadOnlyComposable get() = LocalSnipColors.current
}

@Composable
fun SnipTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors

    // Material still paints the surfaces our own tokens don't reach — dialogs,
    // ripples, text-field internals — so it gets the same palette rather than
    // its own defaults. No dynamic colour: the whole point is that the app
    // looks like Substack, not like the wallpaper.
    val material = if (darkTheme) {
        darkColorScheme(
            primary = colors.mark,
            background = colors.paper,
            surface = colors.paper,
            surfaceVariant = colors.tint,
            onBackground = colors.ink,
            onSurface = colors.ink,
            onSurfaceVariant = colors.muted,
            outlineVariant = colors.hair,
        )
    } else {
        lightColorScheme(
            primary = colors.mark,
            background = colors.paper,
            surface = colors.paper,
            surfaceVariant = colors.tint,
            onBackground = colors.ink,
            onSurface = colors.ink,
            onSurfaceVariant = colors.muted,
            outlineVariant = colors.hair,
        )
    }

    CompositionLocalProvider(LocalSnipColors provides colors) {
        MaterialTheme(
            colorScheme = material,
            typography = SnipTypography,
            content = content,
        )
    }
}
