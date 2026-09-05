package app.snips.capture

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.snips.R
import app.snips.data.SnipDatabase
import app.snips.work.EnrichWorker
import app.snips.ui.theme.SnipTheme
import kotlinx.coroutines.launch

/**
 * The one sheet all three §4 entry points converge on. It always shows what
 * was parsed before anything is written, so a bad parse is visible and
 * fixable rather than silently saved.
 */
class CaptureActivity : ComponentActivity() {

    private val viewModel: CaptureViewModel by lazy {
        val dao = SnipDatabase.get(applicationContext).snips()
        ViewModelProvider(
            this,
            viewModelFactory { initializer { CaptureViewModel(dao) } },
        )[CaptureViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sharedText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            ?: intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()

        // The Substack app sends the real post title in EXTRA_TITLE and no
        // EXTRA_SUBJECT; Chrome sends its own share label in both. parseShare
        // decides which of those is worth keeping.
        val sharedTitle = intent.getStringExtra(Intent.EXTRA_TITLE)
            ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)

        setContent {
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                viewModel.load(
                    sharedText = sharedText,
                    sharedTitle = sharedTitle,
                    // Reading the clipboard raises a system toast on Android 12+,
                    // so it is only consulted when it could actually supply the
                    // missing half — a share with no link, or the Substack app's
                    // bare post link with the passage sitting on the clipboard.
                    clipboardText = if (shareNeedsClipboard(sharedText)) readClipboard() else null,
                )
            }

            SnipTheme {
                CaptureSheet(
                    viewModel = viewModel,
                    onCancel = { finish() },
                    onSave = {
                        scope.launch {
                            val id = viewModel.save()
                            if (id != null) {
                                // §5: the save is already done. The fetch happens
                                // afterwards, on its own time, and the card fills
                                // in when it returns.
                                EnrichWorker.enqueue(this@CaptureActivity, id)
                                Toast.makeText(this@CaptureActivity, R.string.snip_saved, Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                    },
                )
            }
        }
    }

    private fun readClipboard(): String? {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
        return clipboard.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(this)
            ?.toString()
    }
}
