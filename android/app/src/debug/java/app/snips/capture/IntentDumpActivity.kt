package app.snips.capture

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.snips.ui.theme.SnipTheme

/**
 * Step 1 of HANDOVER.md §7 — a throwaway.
 *
 * §4 ends with "don't trust this document over what the intents contain."
 * This screen is how we find out: it renders every field of the incoming
 * intent verbatim, plus the handful of derived signals the capture logic
 * will actually branch on, and copies the whole thing to the clipboard so
 * a real capture on a real phone can be pasted somewhere useful.
 *
 * Lives in src/debug, so it never reaches a release build. Delete it once the
 * two real dumps — Substack app and Chrome — have settled §4.
 */
class IntentDumpActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val report = describe(intent)

        setContent {
            SnipTheme {
                Scaffold { insets ->
                    DumpScreen(report, Modifier.padding(insets))
                }
            }
        }
    }
}

@Composable
private fun DumpScreen(report: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("Intent dump", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Scaffold only. Capture logic lands in step 2.",
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = SnipTheme.colors.muted,
        )

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = SnipTheme.colors.hair)
        Spacer(Modifier.height(12.dp))

        Text(report, fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 18.sp)

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { copyToClipboard(context, report) }) { Text("Copy dump") }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Snips intent dump", text))
    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
}

/**
 * Everything we might conceivably need, in a form that survives a paste into
 * a chat window. Ordering matters: the raw intent first, so a surprise is
 * visible even when the derived signals below read it wrong.
 */
private fun describe(intent: Intent): String = buildString {
    appendLine("action  = ${intent.action}")
    appendLine("type    = ${intent.type}")
    appendLine("scheme  = ${intent.scheme}")
    appendLine("data    = ${intent.dataString}")
    appendLine("categories = ${intent.categories?.joinToString() ?: "(none)"}")
    appendLine()

    val extras = intent.extras
    if (extras == null || extras.isEmpty) {
        appendLine("extras: (none)")
    } else {
        appendLine("extras (${extras.size()}):")
        for (key in extras.keySet()) {
            @Suppress("DEPRECATION") // typed getters need a class per key; this dump wants all of them
            val value = extras.get(key)
            appendLine("  $key")
            appendLine("    type  = ${value?.javaClass?.name ?: "null"}")
            appendLine("    value = ${format(value)}")
        }
    }

    appendLine()
    appendLine("--- derived (HANDOVER §4) ---")

    val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        ?: intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()

    val url = text?.let { URL_IN_TEXT.find(it)?.value }
    appendLine("url found in text     = ${url ?: "(none)"}")
    appendLine("has #:~:text fragment = ${url?.contains(TEXT_FRAGMENT) == true}")
    appendLine("fragment form         = ${fragmentForm(url)}")
    appendLine("readonly process_text = ${intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false)}")
    appendLine("subject               = ${intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: "(none)"}")
    appendLine("referrer              = ${intent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_REFERRER) ?: "(none)"}")
}

private fun format(value: Any?): String = when (value) {
    null -> "null"
    is CharSequence -> "\"${value.toString().replace("\n", "\\n")}\""
    is Array<*> -> value.joinToString(prefix = "[", postfix = "]") { format(it) }
    else -> value.toString()
}

/**
 * Which of the two §4a fragment shapes we got. `start,end` matters because a
 * quote rebuilt from it would be missing its middle — that's what the
 * `fragmentTruncated` flag in the data model exists to record.
 */
private fun fragmentForm(url: String?): String {
    val fragment = url?.substringAfter(TEXT_FRAGMENT, missingDelimiterValue = "") ?: ""
    return when {
        fragment.isEmpty() -> "(none)"
        // Commas separate start,end — but the optional prefix-,quote,-suffix
        // syntax uses them too, so report the raw shape rather than guessing.
        fragment.contains(',') -> "start,end or prefix/suffix — raw: $fragment"
        else -> "single quote — raw: $fragment"
    }
}

private const val TEXT_FRAGMENT = "#:~:text="
private val URL_IN_TEXT = Regex("""https?://\S+""")
