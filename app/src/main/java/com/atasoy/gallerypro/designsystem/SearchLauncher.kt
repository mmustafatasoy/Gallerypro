package com.atasoy.gallerypro.designsystem

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Sekme çubuğundaki arama dairesinin açacağı genel arama ekranı. `GalleryRoot` sağlar;
 * sağlanmamışsa (önizleme, test) sekme çubuğu kendisine verilen işlevi kullanır.
 */
val LocalSearchLauncher = staticCompositionLocalOf<(() -> Unit)?> { null }
