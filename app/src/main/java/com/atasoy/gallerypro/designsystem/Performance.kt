package com.atasoy.gallerypro.designsystem

import android.app.ActivityManager
import android.content.Context

/**
 * Cihazın gücüne göre cam efektleri. Zayıf cihazda (az RAM, az çekirdek veya küçük bellek payı)
 * kırılma ve kademeli bulanıklık kapanır, düz bulanıklık kalır; kaydırma akıcı olur.
 * Uygulama açılırken bir kez [init] çağrılır.
 */
object GalleryPerformance {

    @Volatile
    var reducedEffects: Boolean = false
        private set

    fun init(context: Context) {
        val am = context.getSystemService(ActivityManager::class.java)
        val lowRam = am?.isLowRamDevice == true
        val fewCores = Runtime.getRuntime().availableProcessors() < 6
        val smallHeap = (am?.memoryClass ?: 256) < 192
        reducedEffects = lowRam || fewCores || smallHeap
    }
}
