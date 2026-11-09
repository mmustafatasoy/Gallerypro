package com.atasoy.gallerypro

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import com.atasoy.gallerypro.data.GalleryStores
import com.atasoy.gallerypro.data.collections.SectionPrefs
import com.atasoy.gallerypro.data.edit.EditStore
import com.atasoy.gallerypro.data.media.CaptionStore
import com.atasoy.gallerypro.data.media.GalleryImageLoader
import com.atasoy.gallerypro.data.media.HiddenStore
import com.atasoy.gallerypro.data.media.MediaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class GalleryApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/**
 * Uygulama genelindeki tekil nesneler. Şimdilik elle bağlanır;
 * Room ve yapay zekâ modülleri gelince Hilt'e taşınacak.
 */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val hidden = HiddenStore(context)
    val mediaRepository = MediaRepository(context, appScope, hidden)
    val imageLoader: ImageLoader = GalleryImageLoader.create(context)
    val captions = CaptionStore(context)
    val edits = EditStore(context)
    val sections = SectionPrefs(context)
    val stores = GalleryStores(captions, edits, hidden)
}
