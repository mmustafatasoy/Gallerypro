package com.atasoy.gallerypro.feature.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atasoy.gallerypro.data.collections.CollectionsContent
import com.atasoy.gallerypro.data.collections.CollectionsLogic
import com.atasoy.gallerypro.data.collections.SectionPrefs
import com.atasoy.gallerypro.data.collections.SectionSetting
import com.atasoy.gallerypro.data.media.MediaItem
import com.atasoy.gallerypro.data.media.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId

class CollectionsViewModel(
    private val repository: MediaRepository,
    private val sectionPrefs: SectionPrefs,
    private val zone: ZoneId = ZoneId.systemDefault()
) : ViewModel() {

    val content: StateFlow<CollectionsContent> =
        combine(repository.items, repository.hiddenItems, repository.trashed) { items, hidden, trashed ->
            if (items == null) {
                CollectionsContent(loading = true)
            } else {
                CollectionsLogic.build(items, hidden.size, trashed.size, System.currentTimeMillis(), zone)
            }
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CollectionsContent())

    val sections: StateFlow<List<SectionSetting>> = sectionPrefs.sections

    fun setSections(list: List<SectionSetting>) = sectionPrefs.set(list)

    /** Bir koleksiyonun öğeleri; MediaStore değişince kendiliğinden güncellenir. */
    fun items(key: String): Flow<List<MediaItem>> =
        combine(repository.items, repository.hiddenItems, repository.trashed) { items, hidden, trashed ->
            CollectionsLogic.itemsFor(key, items.orEmpty(), hidden, trashed, System.currentTimeMillis(), zone)
        }.flowOn(Dispatchers.Default)
}
