package app.snips.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import app.snips.ui.theme.PassageStyle
import app.snips.ui.theme.SnipTheme

/**
 * §4: "shows the parsed result before saving, so a bad parse is always
 * visible and fixable."
 *
 * Plain by design — step 2 proves capture works end to end; the card design
 * lands in step 3.
 */
@Composable
fun CaptureSheet(
    viewModel: CaptureViewModel,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    val draft = viewModel.draft

    Scaffold { insets ->
        Column(
            Modifier
                .padding(insets)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Text("Save snip", style = MaterialTheme.typography.titleMedium)

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = draft.text,
                onValueChange = viewModel::onTextChange,
                label = { Text("Passage") },
                textStyle = PassageStyle.copy(color = SnipTheme.colors.ink),
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )

            if (draft.fragmentTruncated) {
                Hint(
                    "Only the start and end of this passage came through the link. " +
                        "The middle is missing — edit it and the link is rebuilt from what you keep.",
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = draft.url,
                onValueChange = viewModel::onUrlChange,
                label = { Text("Article link") },
                textStyle = TextStyle(fontSize = MaterialTheme.typography.bodyMedium.fontSize),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            when (draft.urlSource) {
                UrlSource.CLIPBOARD ->
                    Hint("Taken from your clipboard — check it's the right article.")
                UrlSource.RECENT_SNIP ->
                    Hint("Borrowed from the snip you saved a moment ago — check it's the right article.")
                UrlSource.NONE ->
                    Hint("No link came through. You can paste one, or save without it.")
                UrlSource.SHARED -> Unit
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = viewModel.note,
                onValueChange = viewModel::onNoteChange,
                label = { Text("Note (optional)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(24.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = onCancel) { Text("Cancel") }
                Spacer(Modifier.weight(1f))
                Button(onClick = onSave, enabled = viewModel.canSave) { Text("Save") }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = SnipTheme.colors.muted,
        modifier = Modifier.padding(top = 6.dp),
    )
}
