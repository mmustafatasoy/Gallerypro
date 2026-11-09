package com.atasoy.gallerypro.data

import androidx.compose.runtime.staticCompositionLocalOf
import com.atasoy.gallerypro.data.edit.EditStore
import com.atasoy.gallerypro.data.media.CaptionStore
import com.atasoy.gallerypro.data.media.HiddenStore

/** Ekranların ortak kullandığı küçük depolar; GalleryRoot'ta bir kez sağlanır. */
class GalleryStores(
    val captions: CaptionStore,
    val edits: EditStore,
    val hidden: HiddenStore
)

val LocalGalleryStores = staticCompositionLocalOf<GalleryStores> {
    error("GalleryStores sağlanmadı")
}
