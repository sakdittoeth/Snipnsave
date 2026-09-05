package app.snips.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.snips.capture.fallbackPublication
import app.snips.data.Snip
import app.snips.ui.theme.NoteStyle
import app.snips.ui.theme.PassageStyle
import app.snips.ui.theme.SnipTheme
import coil.compose.AsyncImage

/**
 * §6's anatomy, top to bottom: publication logo, name and relative timestamp /
 * the passage behind the orange rule / an optional note in italic muted / post
 * title and byline / actions.
 *
 * The inversion is the whole idea. Substack's feed makes the title the hero;
 * here the passage is, and the title drops to the footer as attribution. You
 * already know why you saved it — you need to see *what* you saved.
 *
 * One departure from §6, asked for after seeing it running: the cover is the
 * card's background rather than a thumbnail beside the title, echoing the
 * quote cards Substack itself shares. Still no elevation and no shadow —
 * hairlines divide the list, so it reads as continuous with the feed.
 */
@Composable
fun SnipCard(
    snip: Snip,
    onRead: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SnipTheme.colors

    Box(modifier.fillMaxWidth()) {

        // A cover is an arbitrary photograph, so the ground is pulled most of
        // the way back to the theme's own paper before ink is set on it —
        // otherwise 18sp Spectral lands on whatever happens to be in the
        // image. Lower coverScrim() to let more of the picture through.
        if (snip.coverUrl.isNotEmpty()) {
            AsyncImage(
                model = snip.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            Box(
                Modifier
                    .matchParentSize()
                    .background(colors.paper.copy(alpha = coverScrim())),
            )
        }

        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {

            // ---- attribution line ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                // The hair-coloured circle is what shows before enrichment
                // returns, and if it never does. Coil caches to disk, so a logo
                // is fetched once rather than every time the row scrolls past.
                AsyncImage(
                    model = snip.logoUrl.ifEmpty { null },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
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
                // Pushes the timestamp to the right edge. Without it the two read
                // as one phrase — "Savageminds Today" — rather than as a
                // publication on one side and when you saved it on the other.
                Spacer(Modifier.weight(1f))
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

            Spacer(Modifier.height(6.dp))

            // ---- actions ----
            // "Read in context" is offered only when there is a link to follow —
            // a snip saved without one is still worth keeping, but the action
            // would do nothing.
            Row {
                if (snip.url.isNotEmpty()) {
                    TextButton(onClick = onRead, contentPadding = ActionPadding) {
                        Text(
                            "Read in context",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.mark,
                        )
                    }
                }
                TextButton(onClick = onCopy, contentPadding = ActionPadding) {
                    Text("Copy", style = MaterialTheme.typography.labelLarge, color = colors.muted)
                }
                // No confirmation dialog: undo is the gentler answer, and it
                // doesn't interrupt anyone who meant it.
                TextButton(onClick = onDelete, contentPadding = ActionPadding) {
                    Text("Delete", style = MaterialTheme.typography.labelLarge, color = colors.muted)
                }
            }
        }
    }
}

/**
 * How much paper sits between an arbitrary cover photograph and the passage.
 *
 * The light theme needs the heavier scrim: dark ink over a bright, busy image
 * loses legibility faster than light ink over a darkened one.
 */
@Composable
private fun coverScrim(): Float = if (isSystemInDarkTheme()) 0.80f else 0.88f

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
                publication = "Savage Minds",
                title = "Technofeudalism and the Future of Capitalism",
                author = "Yanis Varoufakis",
                savedAt = System.currentTimeMillis() - 86_400_000L * 3,
            ),
            onRead = {},
            onCopy = {},
            onDelete = {},
        )
    }
}
