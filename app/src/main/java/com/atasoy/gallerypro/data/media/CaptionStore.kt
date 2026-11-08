package com.atasoy.gallerypro.data.media

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Fotoğraflara yazılan başlıklar. Sadece uygulamanın kendi deposunda tutulur,
 * fotoğraf dosyasına dokunulmaz. 5. aşamada (arama) Room veritabanına taşınacak.
 */
class CaptionStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("captions", Context.MODE_PRIVATE)

    private val _captions = MutableStateFlow(load())
    val captions: StateFlow<Map<Long, String>> = _captions.asStateFlow()

    fun caption(id: Long): String = _captions.value[id].orEmpty()

    fun set(id: Long, text: String) {
        val clean = text.trim()
        _captions.update { if (clean.isEmpty()) it - id else it + (id to clean) }
        prefs.edit {
            if (clean.isEmpty()) remove(id.toString()) else putString(id.toString(), clean)
        }
    }

    private fun load(): Map<Long, String> = prefs.all.mapNotNull { (key, value) ->
        val id = key.toLongOrNull()
        if (id != null && value is String) id to value else null
    }.toMap()
}
