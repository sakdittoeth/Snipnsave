package app.snips.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.snips.capture.fallbackPublication
import app.snips.data.Snip
import app.snips.ui.theme.NoteStyle
import app.snips.ui.theme.PassageStyle
import app.snips.ui.theme.SnipTheme

/**
 * §6, top to bottom: publication logo + name + relative timestamp / passage
 * with the orange left rule / optional note in italic muted / post title and
 * byline with the cover thumbnail right-aligned / actions.
 *
 * The inversion is the whole idea. Substack's feed makes the title the hero;
 * here the passage is, and the title drops to the footer as attribution. You
 * already know why you saved it — you need to see *what* you saved.
 *
 * No Material card, no elevation, no shadow: cards are divided by hairlines
 * so the library reads as continuous with the feed it came from.
 */
@Composable
fun SnipCard(
    snip: Snip,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SnipTheme.colors

    Column(modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {

        // ---- attribution line ----
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Placeholder until the metadata worker fetches a real logo in step 5.
            Box(
                Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(colors.hair),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = snip.publication.ifEmpty { fallbackPublication(snip.url) },
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = relativeTime(snip.savedAt),
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted,
            )
        }

        Spacer(Modifier.height(12.dp))

        // ---- the passage, and the one place the orange belongs ----
        // IntrinsicSize.Min measures the passage first, so the rule beside it
        // can take exactly its height — no guessing, no clipping on a long quote.
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(colors.mark),
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = snip.text,
                style = PassageStyle,
                color = colors.ink,
            )
        }

        if (snip.note.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(snip.note, style = NoteStyle, color = colors.muted)
        }

        Spacer(Modifier.height(14.dp))

        // ---- attribution footer ----
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = snip.title.ifEmpty { "Untitled post" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (snip.author.isNotEmpty()) {
                    Text(
                        text = snip.author,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            // The cover arrives with enrichment in step 5; until then the
            // footer simply closes up rather than holding an empty box.
            if (snip.coverUrl.isNotEmpty()) {
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.tint),
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // ---- actions ----
        // "Read in context" arrives with Custom Tabs in step 4, delete in step 6.
        Row {
            TextButton(onClick = onCopy, contentPadding = ActionPadding) {
                Text("Copy", style = MaterialTheme.typography.labelLarge, color = colors.muted)
            }
        }
    }
}

private val ActionPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)

@Preview(showBackground = true)
@Composable
private fun SnipCardPreview() {
    SnipTheme {
        SnipCard(
            snip = Snip(
                id = "1",
                text = "Rentiers extract; capitalists invest. That distinction is doing " +
                    "all of the work in this argument, and almost none of the explaining.",
                url = "https://savageminds.substack.com/p/technofeudalism-and-the-future-of",
                note = "Worth re-reading before writing the section on platform rents.",
                publication = "Savageminds",
                title = "Technofeudalism and the Future of Capitalism",
                savedAt = System.currentTimeMillis() - 86_400_000L * 3,
            ),
            onCopy = {},
        )
    }
}
