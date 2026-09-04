package app.snips.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.snips.data.Snip
import app.snips.ui.theme.PassageStyle
import app.snips.ui.theme.SnipTheme

@Composable
fun LibraryScreen(
    snips: List<Snip>,
    onRead: (Snip) -> Unit,
    onCopy: (Snip) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (snips.isEmpty()) {
        EmptyLibrary(modifier)
        return
    }

    LazyColumn(modifier.fillMaxSize()) {
        items(snips, key = { it.id }) { snip ->
            SnipCard(snip = snip, onRead = { onRead(snip) }, onCopy = { onCopy(snip) })
            // Hairlines, not elevation — §6.
            HorizontalDivider(color = SnipTheme.colors.hair)
        }
    }
}

/**
 * Set in the passage face rather than the chrome face, so the first thing an
 * empty library shows is still the voice the cards will have.
 */
@Composable
private fun EmptyLibrary(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Your first snip goes here.",
            style = PassageStyle,
            color = SnipTheme.colors.ink,
            textAlign = TextAlign.Center,
        )
        Text(
            "Select a passage while you're reading, then share it to Snips.",
            style = MaterialTheme.typography.bodySmall,
            color = SnipTheme.colors.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LibraryPreview() {
    val now = System.currentTimeMillis()
    SnipTheme {
        LibraryScreen(
            snips = listOf(
                Snip(
                    id = "1",
                    text = "Rentiers extract; capitalists invest. That distinction is doing all of " +
                        "the work in this argument, and almost none of the explaining.",
                    url = "https://savageminds.substack.com/p/technofeudalism-and-the-future-of",
                    note = "Worth re-reading before the section on platform rents.",
                    publication = "Savageminds",
                    title = "Technofeudalism and the Future of Capitalism",
                    savedAt = now - 86_400_000L * 3,
                ),
                Snip(
                    id = "2",
                    text = "The best writing advice is unusable until you have written enough to " +
                        "recognise what it is describing.",
                    url = "https://astral-codex-ten.substack.com/p/on-prose",
                    publication = "Astral Codex Ten",
                    title = "On Prose",
                    savedAt = now - 86_400_000L * 12,
                ),
            ),
            onRead = {},
            onCopy = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyLibraryPreview() {
    SnipTheme { EmptyLibrary() }
}
