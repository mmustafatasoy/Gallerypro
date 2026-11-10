package com.atasoy.gallerypro.feature.search

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atasoy.gallerypro.data.clip.ClipTextEncoder
import com.atasoy.gallerypro.data.clip.VectorEntry
import com.atasoy.gallerypro.data.clip.VectorMath
import com.atasoy.gallerypro.data.index.IndexStatus
import com.atasoy.gallerypro.data.index.IndexStore
import com.atasoy.gallerypro.data.index.SearchDoc
import com.atasoy.gallerypro.data.index.SearchEngine
import com.atasoy.gallerypro.data.media.CaptionStore
import com.atasoy.gallerypro.data.media.MediaItem
import com.atasoy.gallerypro.data.media.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId

/** Son aramalar (en fazla 8), cihazda. */
class SearchHistory(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("search", Context.MODE_PRIVATE)
    private val _recent = MutableStateFlow(load())
    val recent: StateFlow<List<String>> = _recent.asStateFlow()

    fun add(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        val next = (listOf(q) + _recent.value.filterNot { it.equals(q, ignoreCase = true) }).take(Max)
        _recent.value = next
        prefs.edit().putString(Key, next.joinToString("\n")).apply()
    }

    fun clear() {
        _recent.value = emptyList()
        prefs.edit().remove(Key).apply()
    }

    private fun load(): List<String> =
        prefs.getString(Key, null).orEmpty().split('\n').filter { it.isNotBlank() }.take(Max)

    private companion object {
        const val Key = "recent"
        const val Max = 8
    }
}

/**
 * Arama ekranının durumu. Dizin, başlıklar ve medya listesi birleşip aranabilir belgelere
 * dönüşür (yalnızca ekran açıkken); sorgu 300 ms durunca aranır. Sözcükle bulunanlar önce,
 * ardından cümleyle arama modelinin (CLIP) anlamca benzer bulduğu fotoğraflar gelir.
 */
@OptIn(FlowPreview::class)
class SearchViewModel(
    repository: MediaRepository,
    private val index: IndexStore,
    captions: CaptionStore,
    private val history: SearchHistory,
    private val clipText: ClipTextEncoder,
    private val zone: ZoneId = ZoneId.systemDefault()
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val docs: StateFlow<List<SearchDoc>?> =
        combine(repository.items, index.entries, captions.captions) { items, entries, caps ->
            if (items == null) null else SearchEngine.prepare(items, entries, caps)
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** null: sorgu boş (öneriler gösterilir). */
    val results: StateFlow<List<MediaItem>?> =
        combine(_query.debounce(300), docs, index.vectors) { q, d, vectors ->
            if (q.isBlank() || d == null) {
                null
            } else {
                SearchEngine.merge(SearchEngine.search(q, d, zone), semantic(q, d, vectors))
            }
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val suggestions: StateFlow<List<String>> =
        index.entries
            .map { SearchEngine.topLabels(it.values, 10) }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Cümleyle arama modeli APK'da var mı (yerel derlemelerde olmayabilir). */
    val semanticAvailable: Boolean get() = clipText.available

    /** Anlam vektörü çıkarılmış öğe sayısı. */
    val vectorCount: StateFlow<Int> =
        index.vectors.map { it.size }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val status: StateFlow<IndexStatus> = index.status
    /** Tamamen incelenmiş öğe sayısı: etiketler ve (model varsa) anlam vektörü hazır. */
    val indexedCount: StateFlow<Int> =
        combine(index.entries, index.vectors) { entries, vectors ->
            if (!clipText.available) entries.size else entries.keys.count { it in vectors }
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val mediaCount: StateFlow<Int> =
        repository.items.map { it?.size ?: 0 }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val recent: StateFlow<List<String>> = history.recent

    init {
        viewModelScope.launch(Dispatchers.IO) { index.ensureLoaded() }
    }

    private fun semantic(query: String, docs: List<SearchDoc>, vectors: Map<Long, VectorEntry>): List<MediaItem> {
        if (vectors.isEmpty()) return emptyList()
        val text = SearchEngine.semanticText(query)
        if (text.isEmpty()) return emptyList()
        val q = clipText.embed(text) ?: return emptyList()
        val parsed = SearchEngine.parse(query)
        val now = System.currentTimeMillis()
        val byId = HashMap<Long, MediaItem>(docs.size * 2)
        val candidates = ArrayList<Pair<Long, FloatArray>>(vectors.size)
        for (doc in docs) {
            val v = vectors[doc.item.id] ?: continue
            if (!SearchEngine.matchesFilters(doc.item, parsed, zone, now)) continue
            byId[doc.item.id] = doc.item
            candidates += doc.item.id to v.vector
        }
        return VectorMath.nearest(q, candidates).mapNotNull { byId[it.first] }
    }

    /** Başka bir ekrandan (Bilgi panelindeki etiket) gelen ve arama açılınca uygulanacak sorgu. */
    private var pendingQuery: String? = null

    fun requestQuery(text: String) {
        pendingQuery = text
    }

    fun takePendingQuery(): String? = pendingQuery.also { pendingQuery = null }

    fun setQuery(text: String) {
        _query.value = text
    }

    fun remember(query: String) = history.add(query)

    fun clearRecent() = history.clear()
}
