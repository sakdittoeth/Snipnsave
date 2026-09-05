package app.snips.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.snips.data.Snip
import app.snips.data.SnipDao
import app.snips.data.exportSnips
import app.snips.data.importSnips
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Holds the search query and the one snip an undo could bring back.
 */
class LibraryViewModel(private val dao: SnipDao) : ViewModel() {

    var query by mutableStateOf("")
        private set

    private val queryFlow = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val snips: StateFlow<List<Snip>> = queryFlow
        .flatMapLatest { dao.search(it.trim()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(value: String) {
        query = value
        queryFlow.value = value
    }

    fun clearQuery() = onQueryChange("")

    /**
     * The row is removed now and remembered until the next delete. Undo is a
     * re-insert of the same row, id included, so a snip comes back exactly
     * where it was in the list rather than at the top.
     */
    private var lastDeleted: Snip? = null

    fun delete(snip: Snip) {
        lastDeleted = snip
        viewModelScope.launch { dao.delete(snip) }
    }

    fun undoDelete() {
        val snip = lastDeleted ?: return
        lastDeleted = null
        viewModelScope.launch { dao.insert(snip) }
    }

    /** @return the file's contents, and how many snips are in it. */
    suspend fun exportPayload(): Pair<String, Int> {
        val all = dao.all()
        return exportSnips(all) to all.size
    }

    /**
     * @return how many snips the file yielded, or null if it wasn't readable
     *   as a Snips export at all.
     */
    suspend fun import(json: String): Int? = try {
        val snips = importSnips(json)
        if (snips.isNotEmpty()) dao.insertAll(snips)
        snips.size
    } catch (_: Exception) {
        null
    }
}
