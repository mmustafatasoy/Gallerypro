package com.atasoy.gallerypro.designsystem

import android.app.ActivityManager
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Cam efektlerinin gücü. Zayıf cihazda (az RAM, az çekirdek veya küçük bellek payı) ya da kullanıcı
 * Ayarlar'dan kapattıysa kırılma ve kademeli bulanıklık kapanır, düz bulanıklık kalır; kaydırma akıcı olur.
 * Uygulama açılırken bir kez [init] çağrılır. Seçim cihazda saklanır.
 */
object GalleryPerformance {

    private const val Prefs = "performance"
    private const val KeyEffects = "fullEffects"

    /** Cihaz kendiliğinden zayıf sayıldı mı (bilgi için). */
    @Volatile
    var weakDevice: Boolean = false
        private set

    /** Kullanıcının seçimi: tam cam efektleri açık mı. Compose durumu; değişince cam yüzeyler yeniden çizilir. */
    var fullEffects: Boolean by mutableStateOf(true)
        private set

    val reducedEffects: Boolean get() = !fullEffects

    fun init(context: Context) {
        val am = context.getSystemService(ActivityManager::class.java)
        val lowRam = am?.isLowRamDevice == true
        val fewCores = Runtime.getRuntime().availableProcessors() < 6
        val smallHeap = (am?.memoryClass ?: 256) < 192
        weakDevice = lowRam || fewCores || smallHeap
        val prefs = context.applicationContext.getSharedPreferences(Prefs, Context.MODE_PRIVATE)
        fullEffects = prefs.getBoolean(KeyEffects, !weakDevice)
    }

    fun setFullEffects(context: Context, enabled: Boolean) {
        fullEffects = enabled
        context.applicationContext.getSharedPreferences(Prefs, Context.MODE_PRIVATE)
            .edit().putBoolean(KeyEffects, enabled).apply()
    }
}
