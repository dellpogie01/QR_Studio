@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.juanmediahubph.qrstudio.feature.history

import android.graphics.Bitmap
import android.util.LruCache
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanmediahubph.qrstudio.core.qr.QrPainter
import com.juanmediahubph.qrstudio.domain.model.HistoryFilter
import com.juanmediahubph.qrstudio.domain.model.SavedQr
import com.juanmediahubph.qrstudio.domain.qr.EncodeResult
import com.juanmediahubph.qrstudio.domain.qr.QrEncoder
import com.juanmediahubph.qrstudio.domain.repository.QrRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HistoryUiState(
    val items: List<SavedQr> = emptyList(),
    val query: String = "",
    val filter: HistoryFilter = HistoryFilter.All,
    val selectedIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true,
)

sealed interface HistoryEvent {
    data class Deleted(val count: Int) : HistoryEvent
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: QrRepository,
) : ViewModel() {

    private val encoder = QrEncoder()
    private val thumbnails = LruCache<String, Bitmap>(THUMBNAIL_CACHE_COUNT)
    private val eventsChannel = Channel<HistoryEvent>(Channel.BUFFERED)
    val events: Flow<HistoryEvent> = eventsChannel.receiveAsFlow()

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(HistoryFilter.All)
    private val selection = MutableStateFlow<Set<Long>>(emptySet())
    private var pendingDeletion: List<SavedQr>? = null

    private val items = combine(query, filter) { search, activeFilter ->
        search to activeFilter
    }.flatMapLatest { (search, activeFilter) ->
        repository.observeHistory(search, activeFilter)
    }

    val state: StateFlow<HistoryUiState> = combine(
        items,
        query,
        filter,
        selection,
    ) { list, search, activeFilter, selected ->
        HistoryUiState(
            items = list,
            query = search,
            filter = activeFilter,
            selectedIds = selected,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState(),
    )

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun clearSearch() {
        query.value = ""
    }

    fun onFilterChange(newFilter: HistoryFilter) {
        filter.value = newFilter
        selection.value = emptySet()
    }

    fun toggleSelection(id: Long) {
        selection.update { current ->
            if (id in current) current - id else current + id
        }
    }

    fun clearSelection() {
        selection.value = emptySet()
    }

    fun toggleFavorite(qr: SavedQr) {
        viewModelScope.launch {
            repository.setFavorite(qr.id, !qr.isFavorite)
        }
    }

    fun setSelectedFavorite(favorite: Boolean) {
        val ids = selection.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { id -> repository.setFavorite(id, favorite) }
            selection.value = emptySet()
        }
    }

    fun delete(qr: SavedQr) {
        deleteWithUndo(listOf(qr))
    }

    fun deleteSelected() {
        val selected = state.value.items.filter { it.id in selection.value }
        selection.value = emptySet()
        if (selected.isNotEmpty()) deleteWithUndo(selected)
    }

    fun undoDelete() {
        val deleted = pendingDeletion ?: return
        pendingDeletion = null
        viewModelScope.launch {
            deleted.forEach { qr -> repository.restore(qr) }
        }
    }

    private fun deleteWithUndo(targets: List<SavedQr>) {
        if (targets.isEmpty()) return
        pendingDeletion = targets
        viewModelScope.launch {
            repository.deleteMany(targets.map { it.id })
            eventsChannel.send(HistoryEvent.Deleted(targets.size))
        }
    }

    suspend fun thumbnail(qr: SavedQr): Bitmap? {
        val key = "${qr.id}:${qr.updatedAt}"
        thumbnails.get(key)?.let { return it }
        return withContext(Dispatchers.Default) {
            val result = encoder.encode(qr.payloadText, qr.style.effectiveErrorCorrection)
            val encoded = (result as? EncodeResult.Success)?.qr ?: return@withContext null
            val bitmap = QrPainter.render(encoded, qr.style, THUMBNAIL_SIZE).bitmap
            if (bitmap != null) thumbnails.put(key, bitmap)
            bitmap
        }
    }

    private companion object {
        const val THUMBNAIL_SIZE = 192
        const val THUMBNAIL_CACHE_COUNT = 48
    }
}
