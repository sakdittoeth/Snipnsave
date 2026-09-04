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

        setContent {
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                viewModel.load(
                    sharedText = sharedText,
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
                            if (viewModel.save()) {
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
