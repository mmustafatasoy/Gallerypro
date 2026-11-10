package com.atasoy.gallerypro.data.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings

/**
 * "Medya yönetimi" özel erişimi (Android 12+). Verilirse silme, geri alma ve favori için Android'in kendi
 * onay penceresi çıkmaz; yalnızca uygulamanın cam onay kartı görünür.
 */
object ManageMedia {

    fun granted(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && runCatching { MediaStore.canManageMedia(context) }.getOrDefault(false)

    /** Bu Android sürümünde istenebilir mi (Android 12+). */
    val supported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /** Ayarlar'daki izin ekranını açar; açılamazsa false. */
    fun request(context: Context): Boolean {
        if (!supported) return false
        return runCatching {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_MANAGE_MEDIA, Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        }.getOrDefault(false)
    }
}
