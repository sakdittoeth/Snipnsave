package app.snips

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.snips.capture.fallbackPublication
import app.snips.data.Snip
import app.snips.data.SnipDatabase
import app.snips.ui.theme.PassageStyle
import app.snips.ui.theme.SnipTheme
import kotlinx.coroutines.flow.Flow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val snips = SnipDatabase.get(applicationContext).snips().observeAll()

        setContent {
            SnipTheme {
                Scaffold { insets ->
                    Library(snips, Modifier.padding(insets))
                }
            }
        }
    }
}

/**
 * Step 2 only proves the round trip: a snip shared from anywhere shows up
 * here, and survives a restart. The card design — passage as hero, orange
 * rule, attribution in the footer — is step 3.
 */
@Composable
private fun Library(snipsFlow: Flow<List<Snip>>, modifier: Modifier = Modifier) {
    val snips by snipsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    if (snips.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Snips", style = MaterialTheme.typography.titleMedium)
            Text(
                "Select a passage anywhere and share it here.",
                style = MaterialTheme.typography.bodySmall,
                color = SnipTheme.colors.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        return
    }

    LazyColumn(modifier.fillMaxSize()) {
        items(snips, key = { it.id }) { snip ->
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text(
                    snip.publication.ifEmpty { fallbackPublication(snip.url) },
                    style = MaterialTheme.typography.labelSmall,
                    color = SnipTheme.colors.muted,
                )
                Text(
                    snip.text,
                    style = PassageStyle,
                    color = SnipTheme.colors.ink,
                    modifier = Modifier.padding(top = 6.dp),
                )
                if (snip.note.isNotEmpty()) {
                    Text(
                        snip.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = SnipTheme.colors.muted,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (snip.url.isNotEmpty()) {
                    Text(
                        snip.url,
                        style = MaterialTheme.typography.labelSmall,
                        color = SnipTheme.colors.muted,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            HorizontalDivider(color = SnipTheme.colors.hair)
        }
    }
}
