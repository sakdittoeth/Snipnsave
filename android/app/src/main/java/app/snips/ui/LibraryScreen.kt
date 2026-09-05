package app.snips.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onRead: (Snip) -> Unit,
    onCopy: (Snip) -> Unit,
    onDelete: (Snip) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        // The field stays put while a search is running, so clearing it is
        // always within reach. It only disappears for a library with nothing
        // in it at all, where there is nothing to search.
        if (snips.isNotEmpty() || query.isNotEmpty()) {
            SearchField(query = query, onQueryChange = onQueryChange, onClear = onClearQuery)
            HorizontalDivider(color = SnipTheme.colors.hair)
        }

        when {
            snips.isNotEmpty() -> LazyColumn(Modifier.fillMaxSize()) {
                items(snips, key = { it.id }) { snip ->
                    SnipCard(
                        snip = snip,
                        onRead = { onRead(snip) },
                        onCopy = { onCopy(snip) },
                        onDelete = { onDelete(snip) },
                    )
                    // Hairlines, not elevation — §6.
                    HorizontalDivider(color = SnipTheme.colors.hair)
                }
            }
            query.isNotEmpty() -> NoMatches(query)
            else -> EmptyLibrary()
        }
    }
}

/**
 * Set in the passage face rather than the chrome face, so the first thing an
 * empty library shows is still the voice the cards will have.
 */
@Composable
private fun EmptyLibrary(modifier: Modifier = Modifier) {
    Message(
        headline = "Your first snip goes here.",
        detail = "Select a passage while you're reading, then share it to Snips.",
        modifier = modifier,
    )
}

@Composable
private fun NoMatches(query: String, modifier: Modifier = Modifier) {
    Message(
        headline = "Nothing matches “$query”.",
        detail = "Search looks at the passage, the note, the title and who wrote it.",
        modifier = modifier,
    )
}

@Composable
private fun Message(headline: String, detail: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().fillMaxWidth().padding(40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            headline,
            style = PassageStyle,
            color = SnipTheme.colors.ink,
            textAlign = TextAlign.Center,
        )
        Text(
            detail,
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
                    publication = "Savage Minds",
                    title = "Technofeudalism and the Future of Capitalism",
                    author = "Cory Doctorow",
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
            query = "",
            onQueryChange = {},
            onClearQuery = {},
            onRead = {},
            onCopy = {},
            onDelete = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyLibraryPreview() {
    SnipTheme { EmptyLibrary() }
}

@Preview(showBackground = true)
@Composable
private fun NoMatchesPreview() {
    SnipTheme { NoMatches("technofeudalism") }
}
