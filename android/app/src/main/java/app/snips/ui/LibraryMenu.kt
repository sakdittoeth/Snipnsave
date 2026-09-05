package app.snips.ui

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.snips.ui.theme.SnipTheme

/**
 * Export and import live behind an overflow rather than on the surface: they
 * are things a reader does twice a year, and §6 wants the chrome quiet.
 *
 * The trigger is a text glyph rather than an icon so the app takes no icon
 * dependency for one control.
 */
@Composable
fun LibraryMenu(
    onExport: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }

    TextButton(onClick = { open = true }, modifier = modifier) {
        Text("⋮", style = MaterialTheme.typography.titleMedium, color = SnipTheme.colors.muted)

        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Export snips…") },
                onClick = {
                    open = false
                    onExport()
                },
            )
            DropdownMenuItem(
                text = { Text("Import snips…") },
                onClick = {
                    open = false
                    onImport()
                },
            )
        }
    }
}
