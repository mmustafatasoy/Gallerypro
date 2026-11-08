package com.atasoy.gallerypro

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import com.atasoy.gallerypro.data.media.GalleryImageLoader
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
 * Uygulama genelindeki tekil nesneler. 1. aşamada elle bağlanır;
 * Room ve yapay zekâ modülleri gelince Hilt'e taşınacak.
 */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val mediaRepository = MediaRepository(context, appScope)
    val imageLoader: ImageLoader = GalleryImageLoader.create(context)
}
