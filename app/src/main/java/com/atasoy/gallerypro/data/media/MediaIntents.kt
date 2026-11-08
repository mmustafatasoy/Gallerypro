package com.atasoy.gallerypro.data.media

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Başka uygulamalara giden işlemler (paylaş, birlikte aç, duvar kağıdı, pano).
 * Hepsi kullanıcının dokunmasıyla başlar; uygulama kendisi hiçbir yere bir şey göndermez.
 * Uygun uygulama yoksa false döner.
 */

private fun MediaItem.mime(): String = mimeType ?: if (isVideo) "video/*" else "image/*"

private fun Context.startSafely(intent: Intent): Boolean = try {
    startActivity(intent)
    true
} catch (_: ActivityNotFoundException) {
    false
}

/** Sistemin varsayılan görüntüleyicisinde/oynatıcısında açar. */
fun Context.openInSystemViewer(item: MediaItem): Boolean = startSafely(
    Intent(Intent.ACTION_VIEW)
        .setDataAndType(item.contentUri(), item.mime())
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
)

/** "Farklı uygulamada aç": her seferinde seçim penceresi gösterir. */
fun Context.openWith(item: MediaItem, title: String): Boolean = startSafely(
    Intent.createChooser(
        Intent(Intent.ACTION_VIEW)
            .setDataAndType(item.contentUri(), item.mime())
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
        title
    )
)

fun Context.shareItems(items: List<MediaItem>, title: String): Boolean {
    if (items.isEmpty()) return false
    val uris = ArrayList<Uri>(items.map { it.contentUri() })
    val type = when {
        items.all { it.isVideo } -> "video/*"
        items.none { it.isVideo } -> "image/*"
        else -> "*/*"
    }
    val send = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris.first())
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
    }
    send.setType(type).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return startSafely(Intent.createChooser(send, title))
}

/** Duvar kağıdı / kişi fotoğrafı olarak ayarla (sistem seçim penceresi). */
fun Context.setAs(item: MediaItem, title: String): Boolean {
    val intent = Intent(Intent.ACTION_ATTACH_DATA)
        .setDataAndType(item.contentUri(), item.mime())
        .putExtra("mimeType", item.mime())
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return startSafely(Intent.createChooser(intent, title))
}

fun Context.copyToClipboard(item: MediaItem, label: String): Boolean {
    val clipboard = getSystemService(ClipboardManager::class.java) ?: return false
    clipboard.setPrimaryClip(ClipData.newUri(contentResolver, label, item.contentUri()))
    return true
}

/** Konumu kullanıcının harita uygulamasında açar (geo: bağlantısı). */
fun Context.openMap(latitude: Double, longitude: Double, label: String): Boolean {
    val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(label)})")
    return startSafely(Intent(Intent.ACTION_VIEW, uri))
}
