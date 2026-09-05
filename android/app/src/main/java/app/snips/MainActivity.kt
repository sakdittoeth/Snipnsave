package app.snips

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

            // §7 goes through the Storage Access Framework, so the file lands
            // wherever the reader keeps things and the app needs no storage
            // permission of its own.
            val exportFile = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/json"),
            ) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                scope.launch {
                    val (payload, count) = viewModel.exportPayload()
                    val ok = withContext(Dispatchers.IO) { writeTo(uri, payload) }
                    snackbar.showSnackbar(
                        if (ok) resources.getQuantityString(R.plurals.exported, count, count)
                        else getString(R.string.export_failed),
                    )
                }
            }

            val importFile = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument(),
            ) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                scope.launch {
                    val text = withContext(Dispatchers.IO) { readFrom(uri) }
                    val count = text?.let { viewModel.import(it) }
                    snackbar.showSnackbar(
                        if (count == null) getString(R.string.import_failed)
                        else resources.getQuantityString(R.plurals.imported, count, count),
                    )
                }
            }

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
                        onExport = { exportFile.launch(exportFileName()) },
                        // Some file pickers mislabel a .json file as plain
                        // text, so both are accepted rather than hiding the
                        // reader's own export from them.
                        onImport = { importFile.launch(arrayOf("application/json", "text/plain")) },
                        modifier = Modifier.padding(insets),
                    )
                }
            }
        }
    }

    private fun writeTo(uri: Uri, payload: String): Boolean = try {
        contentResolver.openOutputStream(uri)?.use { it.write(payload.toByteArray()) } != null
    } catch (_: Exception) {
        false
    }

    private fun readFrom(uri: Uri): String? = try {
        contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
    } catch (_: Exception) {
        null
    }

    /** Dated, so successive backups sit beside each other rather than clash. */
    private fun exportFileName(): String {
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return "snips-$day.json"
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
