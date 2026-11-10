package com.atasoy.gallerypro.designsystem

import androidx.compose.runtime.staticCompositionLocalOf

/** Genel arama ekranını açar; [query] verilirse o sözcükle arar (Bilgi panelindeki etiketler). */
fun interface SearchLauncher {
    fun open(query: String?)
}

/**
 * Sekme çubuğundaki arama dairesinin açacağı genel arama ekranı. `GalleryRoot` sağlar (arama ekranının
 * içindeki görüntüleyiciye de arama ekranı kendisininkini verir); sağlanmamışsa (önizleme, test)
 * sekme çubuğu kendisine verilen işlevi kullanır.
 */
val LocalSearchLauncher = staticCompositionLocalOf<SearchLauncher?> { null }
