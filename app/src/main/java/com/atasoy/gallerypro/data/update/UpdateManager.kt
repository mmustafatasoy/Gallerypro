package com.atasoy.gallerypro.data.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import com.atasoy.gallerypro.BuildConfig
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

data class AvailableUpdate(
    val version: String,
    val notes: String,
    val sha256: String,
    val sizeBytes: Long
)

sealed interface UpdateCheck {
    data object Idle : UpdateCheck
    data object Checking : UpdateCheck
    data object UpToDate : UpdateCheck
    data class Available(val release: AvailableUpdate) : UpdateCheck
    data object Failed : UpdateCheck
}

sealed interface UpdateDownload {
    data object Idle : UpdateDownload
    data class Downloading(val received: Long, val total: Long) : UpdateDownload
    data class Ready(val id: Long, val version: String) : UpdateDownload
    data object Failed : UpdateDownload
}

/**
 * Yalnızca cihazdan GitHub APK yükleme akışı; hiçbir medya verisini okumaz/göndermez.
 * Google Play AAB'sinde bu sınıf çalıştırılmaz ve ilgili izinler çıkarılır.
 */
class UpdateManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val prefs = context.getSharedPreferences("gallery_sideload_updates", Context.MODE_PRIVATE)
    private val downloads by lazy { context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager }
    private val _check = MutableStateFlow<UpdateCheck>(UpdateCheck.Idle)
    val check: StateFlow<UpdateCheck> = _check
    private val _download = MutableStateFlow<UpdateDownload>(UpdateDownload.Idle)
    val download: StateFlow<UpdateDownload> = _download

    init {
        if (BuildConfig.SIDELOAD_UPDATER) {
            scope.launch(Dispatchers.IO) {
                while (isActive) {
                    refreshDownload()
                    delay(1_500L)
                }
            }
        }
    }

    fun checkNow(force: Boolean = false) {
        if (!BuildConfig.SIDELOAD_UPDATER || _check.value == UpdateCheck.Checking) return
        val last = prefs.getLong("last_check", 0L)
        val now = System.currentTimeMillis()
        if (!force && now >= last && now - last < 30 * 60_000L) {
            val cached = cachedRelease()
            _check.value = if (cached != null && UpdateLogic.isNewer(BuildConfig.VERSION_NAME, cached.version))
                UpdateCheck.Available(cached) else UpdateCheck.UpToDate
            return
        }
        _check.value = UpdateCheck.Checking
        scope.launch(Dispatchers.IO) {
            try {
                val update = fetchLatest()
                prefs.edit().putLong("last_check", System.currentTimeMillis()).apply()
                if (update == null) {
                    clearCached()
                    _check.value = UpdateCheck.UpToDate
                } else {
                    cache(update)
                    _check.value = UpdateCheck.Available(update)
                }
            } catch (_: Exception) {
                _check.value = UpdateCheck.Failed
            }
        }
    }

    fun shouldSuggest(release: AvailableUpdate): Boolean =
        prefs.getString("dismissed", null) != release.version

    fun dismiss(release: AvailableUpdate) {
        prefs.edit().putString("dismissed", release.version).apply()
    }

    private fun fetchLatest(): AvailableUpdate? {
        val address = URL("https://api.github.com/repos/mmustafatasoy/Gallerypro/releases/tags/son-surum")
        val connection = address.openConnection() as HttpURLConnection
        connection.connectTimeout = 12_000
        connection.readTimeout = 12_000
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("User-Agent", "Gallerypro-Android-Updater")
        try {
            if (connection.responseCode != 200) error("GitHub sürüm sorgusu başarısız")
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val version = UpdateLogic.versionOf(json.optString("name")) ?: return null
            if (!UpdateLogic.isNewer(BuildConfig.VERSION_NAME, version)) return null
            val assets = json.getJSONArray("assets")
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.optString("name") != "Gallerypro.apk") continue
                if (!UpdateLogic.allowedUrl(a.optString("browser_download_url"))) return null
                val hash = UpdateLogic.validDigest(a.optString("digest")) ?: return null
                val size = a.optLong("size")
                if (size !in 1_000_000L..2_000_000_000L) return null
                return AvailableUpdate(version, json.optString("body").take(900), hash, size)
            }
            return null
        } finally {
            connection.disconnect()
        }
    }

    private fun cache(r: AvailableUpdate) {
        prefs.edit().putString("cached_version", r.version).putString("cached_notes", r.notes)
            .putString("cached_sha", r.sha256).putLong("cached_size", r.sizeBytes).apply()
    }

    private fun cachedRelease(): AvailableUpdate? {
        val version = prefs.getString("cached_version", null) ?: return null
        val hash = UpdateLogic.validDigest(prefs.getString("cached_sha", null)) ?: return null
        val size = prefs.getLong("cached_size", 0L)
        if (size <= 0L) return null
        return AvailableUpdate(version, prefs.getString("cached_notes", "").orEmpty(), hash, size)
    }

    private fun clearCached() {
        prefs.edit().remove("cached_version").remove("cached_notes")
            .remove("cached_sha").remove("cached_size").apply()
    }

    fun beginDownload(release: AvailableUpdate) {
        if (!BuildConfig.SIDELOAD_UPDATER || _download.value is UpdateDownload.Downloading) return
        if (UpdateLogic.validDigest(release.sha256) == null ||
            !UpdateLogic.isNewer(BuildConfig.VERSION_NAME, release.version) ||
            release.sizeBytes !in 1_000_000L..2_000_000_000L) return
        // Hızlı çift dokunuş iki ayrı indirme başlatmamalı.
        _download.value = UpdateDownload.Downloading(0L, release.sizeBytes)
        scope.launch(Dispatchers.IO) {
            try {
                val old = prefs.getLong("download_id", -1L)
                if (old >= 0) runCatching { downloads.remove(old) }
                val request = DownloadManager.Request(Uri.parse(UpdateLogic.ApkUrl))
                    .setMimeType("application/vnd.android.package-archive")
                    .setTitle("Gallerypro " + release.version)
                    .setDescription("Gallerypro APK")
                    .setAllowedOverMetered(true)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS,
                        "Gallerypro-" + release.version + ".apk")
                val id = downloads.enqueue(request)
                prefs.edit().putLong("download_id", id)
                    .putString("download_sha", release.sha256)
                    .putString("download_version", release.version)
                    .putLong("download_size", release.sizeBytes).apply()
                _download.value = UpdateDownload.Downloading(0L, release.sizeBytes)
            } catch (_: Exception) {
                _download.value = UpdateDownload.Failed
            }
        }
    }

    private fun refreshDownload() {
        val id = prefs.getLong("download_id", -1L)
        if (id < 0L || _download.value is UpdateDownload.Ready) return
        try {
            downloads.query(DownloadManager.Query().setFilterById(id))?.use { c ->
                if (!c.moveToFirst()) {
                    _download.value = UpdateDownload.Failed
                    return
                }
                val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val received = c.getLong(
                    c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val expectedSize = prefs.getLong("download_size", 0L)
                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        val hash = prefs.getString("download_sha", "").orEmpty()
                        if (verify(id, expectedSize, hash)) {
                            _download.value = UpdateDownload.Ready(
                                id, prefs.getString("download_version", "").orEmpty())
                        } else {
                            downloads.remove(id)
                            prefs.edit().remove("download_id").apply()
                            _download.value = UpdateDownload.Failed
                        }
                    }
                    DownloadManager.STATUS_FAILED -> {
                        prefs.edit().remove("download_id").apply()
                        _download.value = UpdateDownload.Failed
                    }
                    else -> _download.value = UpdateDownload.Downloading(received, expectedSize)
                }
            }
        } catch (_: Exception) {
            _download.value = UpdateDownload.Failed
        }
    }

    /** Dosya SHA-256 doğrulanmadan kurulum ekranını hiçbir zaman açma. */
    private fun verify(id: Long, expectedSize: Long, expectedHash: String): Boolean {
        if (expectedSize <= 0L || UpdateLogic.validDigest(expectedHash) == null) return false
        val md = MessageDigest.getInstance("SHA-256")
        var total = 0L
        downloads.openDownloadedFile(id).use { fd ->
            FileInputStream(fd.fileDescriptor).use { stream ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val n = stream.read(buffer)
                    if (n < 0) break
                    md.update(buffer, 0, n)
                    total += n
                    if (total > expectedSize) return false
                }
            }
        }
        val actual = md.digest().joinToString("") {
            "%02x".format(Locale.ROOT, it.toInt() and 0xff)
        }
        return total == expectedSize && actual == expectedHash
    }

    /** Android kurulumu her zaman kullanıcı onayıyla gerçekleşir. */
    fun install() {
        val ready = _download.value as? UpdateDownload.Ready ?: return
        runCatching {
            if (!context.packageManager.canRequestPackageInstalls()) {
                context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + context.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return@runCatching
            }
            val uri = downloads.getUriForDownloadedFile(ready.id)
                ?: error("DownloadManager APK yok")
            context.startActivity(Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure { _download.value = UpdateDownload.Failed }
    }
}
