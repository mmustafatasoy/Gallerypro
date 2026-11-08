package com.atasoy.gallerypro.feature.library

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atasoy.gallerypro.data.media.LibraryFilter
import com.atasoy.gallerypro.data.media.MediaItem
import com.atasoy.gallerypro.data.media.MediaRepository
import com.atasoy.gallerypro.data.media.Period
import com.atasoy.gallerypro.data.media.PeriodGrouper
import com.atasoy.gallerypro.data.media.filteredBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.ZoneId

enum class LibraryMode { Years, Months, All }

/** Izgarada desteklenen sütun sayıları; iki parmakla geçilir. */
val ColumnSteps = listOf(1, 3, 5, 9)

@Immutable
data class LibraryContent(
    val loading: Boolean,
    val items: List<MediaItem>,
    val months: List<Period>,
    val years: List<Period>
)

@Immutable
data class LibraryUiState(
    val mode: LibraryMode = LibraryMode.All,
    val columns: Int = 3,
    val filter: LibraryFilter = LibraryFilter.All,
    val selecting: Boolean = false,
    val selected: Set<Long> = emptySet(),
    val menuOpen: Boolean = false
)

class LibraryViewModel(
    private val repository: MediaRepository,
    private val zone: ZoneId = ZoneId.systemDefault()
) : ViewModel() {

    private val _ui = MutableStateFlow(LibraryUiState())
    val ui: StateFlow<LibraryUiState> = _ui

    private val filterFlow = MutableStateFlow(LibraryFilter.All)

    val content: StateFlow<LibraryContent> =
        combine(repository.items, filterFlow) { all, filter ->
            if (all == null) {
                LibraryContent(loading = true, items = emptyList(), months = emptyList(), years = emptyList())
            } else {
                val items = all.filteredBy(filter)
                LibraryContent(
                    loading = false,
                    items = items,
                    months = PeriodGrouper.months(items, zone),
                    years = PeriodGrouper.years(items, zone)
                )
            }
        }
            .flowOn(Dispatchers.Default)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                LibraryContent(loading = true, items = emptyList(), months = emptyList(), years = emptyList())
            )

    fun setMode(mode: LibraryMode) = _ui.update { it.copy(mode = mode, menuOpen = false) }

    fun zoomIn() = _ui.update { state ->
        val i = ColumnSteps.indexOf(state.columns).coerceAtLeast(0)
        state.copy(columns = ColumnSteps[(i - 1).coerceAtLeast(0)])
    }

    fun zoomOut() = _ui.update { state ->
        val i = ColumnSteps.indexOf(state.columns).coerceAtLeast(0)
        state.copy(columns = ColumnSteps[(i + 1).coerceAtMost(ColumnSteps.lastIndex)])
    }

    fun setFilter(filter: LibraryFilter) {
        filterFlow.value = filter
        _ui.update { it.copy(filter = filter, menuOpen = false, selected = emptySet()) }
    }

    fun toggleMenu() = _ui.update { it.copy(menuOpen = !it.menuOpen) }
    fun closeMenu() = _ui.update { it.copy(menuOpen = false) }

    fun startSelecting(firstId: Long? = null) = _ui.update {
        it.copy(
            selecting = true,
            mode = LibraryMode.All,
            menuOpen = false,
            selected = if (firstId != null) setOf(firstId) else emptySet()
        )
    }

    fun stopSelecting() = _ui.update { it.copy(selecting = false, selected = emptySet()) }

    fun toggle(id: Long) = _ui.update {
        it.copy(selected = if (id in it.selected) it.selected - id else it.selected + id)
    }
}
