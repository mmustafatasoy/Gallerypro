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

/** Var olan MediaStore albümlerinde yalnızca yazılabilen klasörleri göster. */
object AlbumCopyLogic {
    fun supported(path: String, video: Boolean): Boolean {
        val normalized = path.replace('\\', '/').trimStart('/').lowercase(java.util.Locale.ROOT)
        if (normalized == ".." || normalized.contains("../")) return false
        return normalized.startsWith("dcim/") || normalized.startsWith("pictures/") ||
            (video && normalized.startsWith("movies/"))
    }
}

/** IS_PENDING ile yeni kopya oluşturur. Dispatchers.IO üzerinde çağrılır; orijinal değişmez. */
fun Context.copyItemToAlbum(item: MediaItem, albumPath: String): Boolean {
    if (!AlbumCopyLogic.supported(albumPath, item.isVideo)) return false
    val resolver = contentResolver
    val destination = if (item.isVideo) android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                      else android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val original = item.displayName?.takeIf { it.isNotBlank() }
        ?: if (item.isVideo) "video.mp4" else "photo.jpg"
    val dot = original.lastIndexOf('.')
    val name = if (dot > 0) original.substring(0, dot) else original
    val ext = if (dot > 0) original.substring(dot) else ""
    val values = android.content.ContentValues().apply {
        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "${name}_kopya_${System.currentTimeMillis()}${ext}")
        put(android.provider.MediaStore.MediaColumns.MIME_TYPE,
            item.mimeType ?: if (item.isVideo) "video/mp4" else "image/jpeg")
        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, albumPath)
        put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
    }
    var inserted: Uri? = null
    return try {
        val dest = resolver.insert(destination, values) ?: return false
        inserted = dest
        val source = resolver.openInputStream(item.contentUri()) ?: throw java.io.IOException()
        source.use { input ->
            val output = resolver.openOutputStream(dest, "w") ?: throw java.io.IOException()
            output.use { input.copyTo(it, 64 * 1024) }
        }
        val published = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
        }
        if (resolver.update(dest, published, null, null) <= 0) throw java.io.IOException()
        true
    } catch (_: Exception) {
        inserted?.let { runCatching { resolver.delete(it, null, null) } }
        false
    }
}
