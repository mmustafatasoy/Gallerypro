package com.atasoy.gallerypro.designsystem

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Tüm renk, boyut ve hareket değerleri burada. Ekran dosyalarına sayı gömülmez.
 * Değerler proje dokümanındaki "Tasarım dili" bölümüyle birebir aynıdır.
 */
object GalleryColors {
    val Accent = Color(0xFF1F6FEB)
    val Favorite = Color(0xFFFF375F)
    val Delete = Color(0xFFFF453A)
    val EditAccent = Color(0xFFFFD84D)

    val LightBackground = Color(0xFFFFFFFF)
    val DarkBackground = Color(0xFF000000)

    val TextPrimary = Color(0xFF111114)
    val TextSecondary = Color(0xFF55555C)
    val TextTertiary = Color(0xFF6B6B73)
    val IconOnLight = Color(0xFF26262B)

    val CellPlaceholder = Color(0xFFEFEFF2)
    val SelectedTabFill = Color(0x11000000)
}

object GalleryDimens {
    val ScreenPadding = 16.dp
    val TitleStartPadding = 20.dp

    val TopButton = 40.dp
    val ViewerTopButton = 48.dp
    val ToolbarButton = 56.dp
    val TabBarHeight = 64.dp
    val TabBarInner = 5.dp

    val SegmentedHeight = 42.dp
    val SegmentHeight = 34.dp
    val SegmentWidth = 78.dp

    val GridSpacing = 2.dp
    val HeaderHeight = 150.dp
    val BottomFadeHeight = 190.dp

    val SheetCorner = 34.dp
    val CardCorner = 26.dp
    val YearCardHeight = 330.dp
    val MonthCardHeight = 250.dp
    val CardSpacing = 14.dp

    val GlassBlurLight = 22.dp
    val GlassBlurDark = 24.dp
    val GlassBlurSheet = 40.dp
    val LensHeight = 12.dp
    val LensAmount = 24.dp
    val ShadowRadius = 24.dp
}

object GalleryMotion {
    /** Doküman: sönümleme 0,8, sertlik 380. */
    fun <T> standard(): SpringSpec<T> = spring(dampingRatio = 0.8f, stiffness = 380f)

    /** Cam butona basınca hafif esneme. */
    fun <T> press(): SpringSpec<T> = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)

    const val PressedScale = 0.94f
}
