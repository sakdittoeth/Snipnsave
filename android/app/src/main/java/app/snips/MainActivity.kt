package app.snips

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.snips.data.Snip
import app.snips.data.SnipDatabase
import app.snips.ui.LibraryScreen
import app.snips.ui.openInContext
import app.snips.ui.theme.SnipTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val snips = SnipDatabase.get(applicationContext).snips().observeAll()

        setContent {
            val library by snips.collectAsStateWithLifecycle(initialValue = emptyList())

            SnipTheme {
                // The Custom Tab's chrome takes the app's own paper tone, so
                // stepping out to the article doesn't feel like leaving.
                val toolbar = SnipTheme.colors.paper.toArgb()

                Scaffold { insets ->
                    LibraryScreen(
                        snips = library,
                        onRead = { snip -> readInContext(snip, toolbar) },
                        onCopy = ::copyToClipboard,
                        modifier = Modifier.padding(insets),
                    )
                }
            }
        }
    }

    private fun readInContext(snip: Snip, toolbarColor: Int) {
        if (!openInContext(this, snip, toolbarColor)) {
            Toast.makeText(this, R.string.no_browser, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * The prototype's copy format: the passage, then who it belongs to, then
     * where it came from. Enough to paste into anything without stranding the
     * quote from its source.
     */
    private fun copyToClipboard(snip: Snip) {
        val attribution = snip.author.ifEmpty { snip.publication }
        val payload = buildString {
            append("“").append(snip.text).append("”")
            if (attribution.isNotEmpty() || snip.url.isNotEmpty()) append("\n—")
            if (attribution.isNotEmpty()) append(" ").append(attribution)
            if (attribution.isNotEmpty() && snip.url.isNotEmpty()) append(",")
            if (snip.url.isNotEmpty()) append(" ").append(snip.url)
        }

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText(snip.title.ifEmpty { "Snip" }, payload))
        Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
    }
}
