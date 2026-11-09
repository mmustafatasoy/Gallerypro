package com.atasoy.gallerypro.data.edit

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

/**
 * Düzenlenen fotoğrafların ayarları ve orijinal dosyaların yedeği.
 * Orijinal, uygulamanın kendi klasöründe (files/originals/<id>) birebir saklanır;
 * "Sıfırla + Bitti" bu yedeği geri yazar. Ayarlar 5. aşamada Room'a taşınacak.
 */
class EditStore(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("edits", Context.MODE_PRIVATE)
    private val dir = File(appContext.filesDir, "originals")

    private val _editedIds = MutableStateFlow(load())
    val editedIds: StateFlow<Set<Long>> = _editedIds.asStateFlow()

    /** Kayıtlı ayarlar; yedek yoksa (veriler silinmiş) null. */
    fun params(id: Long): EditParams? {
        if (!hasBackup(id)) return null
        return EditParams.decode(prefs.getString(id.toString(), null))
    }

    fun backupFile(id: Long): File = File(dir, id.toString())

    fun hasBackup(id: Long): Boolean = backupFile(id).isFile

    fun ensureDir() {
        if (!dir.isDirectory) dir.mkdirs()
    }

    fun save(id: Long, params: EditParams) {
        prefs.edit { putString(id.toString(), params.encode()) }
        _editedIds.update { it + id }
    }

    fun clear(id: Long) {
        prefs.edit { remove(id.toString()) }
        backupFile(id).delete()
        _editedIds.update { it - id }
    }

    private fun load(): Set<Long> = prefs.all.keys.mapNotNullTo(HashSet()) { it.toLongOrNull() }
}
