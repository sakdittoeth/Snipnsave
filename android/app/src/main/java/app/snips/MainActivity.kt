package app.snips

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.snips.ui.theme.SnipTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SnipTheme {
                Scaffold { insets ->
                    LibraryPlaceholder(Modifier.padding(insets))
                }
            }
        }
    }
}

/**
 * Stands in for the library list until step 3. Deliberately not the real
 * empty state — that gets designed alongside the card, not before it.
 */
@Composable
private fun LibraryPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Snips", style = MaterialTheme.typography.titleMedium)
        Text(
            "Select text anywhere and share it here.\nNothing is saved yet — step 2 wires up storage.",
            style = MaterialTheme.typography.bodySmall,
            color = SnipTheme.colors.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Preview
@Composable
private fun LibraryPlaceholderPreview() {
    SnipTheme { LibraryPlaceholder() }
}
