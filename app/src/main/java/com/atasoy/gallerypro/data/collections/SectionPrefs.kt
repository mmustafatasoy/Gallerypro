package com.atasoy.gallerypro.data.collections

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Koleksiyonlar bölümlerinin sırası ve görünürlüğü.
 * Şimdilik SharedPreferences; 5. aşamada Room ile birlikte DataStore'a geçilecek.
 */
class SectionPrefs(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("collections", Context.MODE_PRIVATE)

    private val _sections = MutableStateFlow(SectionOrder.decode(prefs.getString(KEY, null)))
    val sections: StateFlow<List<SectionSetting>> = _sections.asStateFlow()

    fun set(list: List<SectionSetting>) {
        _sections.value = list
        prefs.edit { putString(KEY, SectionOrder.encode(list)) }
    }

    private companion object {
        const val KEY = "sections"
    }
}
