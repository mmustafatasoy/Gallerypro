package com.atasoy.gallerypro.data.media

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Gizlenen öğelerin kimlikleri. Dosyaya dokunulmaz; öğe Kütüphane ve
 * koleksiyonlardan çıkar, sadece kilitli Gizli albümünde görünür.
 * 5. aşamada Room veritabanına taşınacak.
 */
class HiddenStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("hidden", Context.MODE_PRIVATE)

    private val _ids = MutableStateFlow(load())
    val ids: StateFlow<Set<Long>> = _ids.asStateFlow()

    fun hide(ids: Collection<Long>) = change { it + ids }

    fun unhide(ids: Collection<Long>) = change { it - ids.toSet() }

    private fun change(block: (Set<Long>) -> Set<Long>) {
        _ids.update(block)
        val snapshot = _ids.value
        prefs.edit { putStringSet(KEY, snapshot.mapTo(HashSet()) { it.toString() }) }
    }

    private fun load(): Set<Long> =
        prefs.getStringSet(KEY, emptySet()).orEmpty().mapNotNullTo(HashSet()) { it.toLongOrNull() }

    private companion object {
        const val KEY = "ids"
    }
}
