package com.atasoy.gallerypro.data.media

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Medya listesinin tek kaynağı. MediaStore değişince (yeni fotoğraf, silme)
 * kendiliğinden yeniler. null = henüz yüklenmedi.
 *
 * [items] gizlenenler hariç her şeydir; gizlenenler [hiddenItems], çöptekiler [trashed].
 */
class MediaRepository(
    context: Context,
    private val scope: CoroutineScope,
    hidden: HiddenStore
) {
    private val appContext = context.applicationContext
    private val source = MediaStoreSource(appContext)

    private val _all = MutableStateFlow<List<MediaItem>?>(null)

    val items: StateFlow<List<MediaItem>?> =
        combine(_all, hidden.ids) { all, ids ->
            if (all == null || ids.isEmpty()) all else all.filter { it.id !in ids }
        }.stateIn(scope, SharingStarted.Eagerly, null)

    val hiddenItems: StateFlow<List<MediaItem>> =
        combine(_all, hidden.ids) { all, ids ->
            if (all == null || ids.isEmpty()) emptyList() else all.filter { it.id in ids }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val _trashed = MutableStateFlow<List<MediaItem>>(emptyList())
    val trashed: StateFlow<List<MediaItem>> = _trashed.asStateFlow()

    private var observing = false
    private var reloadJob: Job? = null

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            scheduleReload(debounceMs = 400L)
        }
    }

    /** İzin verildikten sonra çağrılır; ikinci çağrı sadece yeniler. */
    fun start() {
        if (!observing) {
            appContext.contentResolver.registerContentObserver(
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
                true,
                observer
            )
            observing = true
        }
        scheduleReload(debounceMs = 0L)
    }

    fun refresh() = scheduleReload(debounceMs = 0L)

    private fun scheduleReload(debounceMs: Long) {
        reloadJob?.cancel()
        reloadJob = scope.launch(Dispatchers.IO) {
            if (debounceMs > 0) delay(debounceMs)
            val list = runCatching { source.queryAll() }.getOrElse { emptyList() }
            _all.value = list
            _trashed.value = runCatching { source.queryTrashed() }.getOrElse { emptyList() }
        }
    }
}
