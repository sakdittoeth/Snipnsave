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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.snips.data.Snip
import app.snips.data.SnipDatabase
import app.snips.ui.LibraryScreen
import app.snips.ui.LibraryViewModel
import app.snips.ui.openInContext
import app.snips.ui.theme.SnipTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: LibraryViewModel by lazy {
        val dao = SnipDatabase.get(applicationContext).snips()
        ViewModelProvider(
            this,
            viewModelFactory { initializer { LibraryViewModel(dao) } },
        )[LibraryViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val library by viewModel.snips.collectAsStateWithLifecycle()
            val snackbar = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()

            SnipTheme {
                // The Custom Tab's chrome takes the app's own paper tone, so
                // stepping out to the article doesn't feel like leaving.
                val toolbar = SnipTheme.colors.paper.toArgb()

                Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { insets ->
                    LibraryScreen(
                        snips = library,
                        query = viewModel.query,
                        onQueryChange = viewModel::onQueryChange,
                        onClearQuery = viewModel::clearQuery,
                        onRead = { snip -> readInContext(snip, toolbar) },
                        onCopy = ::copyToClipboard,
                        onDelete = { snip ->
                            viewModel.delete(snip)
                            scope.launch {
                                // The row is already gone; this is the way back.
                                val result = snackbar.showSnackbar(
                                    message = getString(R.string.snip_deleted),
                                    actionLabel = getString(R.string.undo),
                                )
                                if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
                            }
                        },
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
