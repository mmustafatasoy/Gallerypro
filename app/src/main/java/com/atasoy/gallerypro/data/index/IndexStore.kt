package com.atasoy.gallerypro.data.index

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.atasoy.gallerypro.data.clip.VectorEntry
import com.atasoy.gallerypro.data.clip.VectorMath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Yapay zekâ dizininin cihazdaki kaydı (uygulamanın kendi SQLite dosyası, `index.db`).
 * Arama için tamamı bellekte de tutulur: 10.000 fotoğrafta birkaç MB, arama milisaniyeler sürer.
 * Dizinleyici ([IndexWorker]) yazar, arama ekranı [entries] akışını okur.
 */
class IndexStore(context: Context) {

    private val db = Helper(context.applicationContext)
    private val lock = Any()
    @Volatile private var loaded = false

    private val _entries = MutableStateFlow<Map<Long, IndexEntry>>(emptyMap())
    val entries: StateFlow<Map<Long, IndexEntry>> = _entries.asStateFlow()

    /** Cümleyle arama için CLIP vektörleri (öğe kimliği → vektör). */
    private val _vectors = MutableStateFlow<Map<Long, VectorEntry>>(emptyMap())
    val vectors: StateFlow<Map<Long, VectorEntry>> = _vectors.asStateFlow()

    private val _status = MutableStateFlow(IndexStatus())
    val status: StateFlow<IndexStatus> = _status.asStateFlow()

    /** İlk okumada diskten yükler. Ana iş parçacığında çağrılmaz. */
    fun ensureLoaded() {
        if (loaded) return
        synchronized(lock) {
            if (loaded) return
            _entries.value = runCatching { readAll() }.getOrDefault(emptyMap())
            _vectors.value = runCatching { readVectors() }.getOrDefault(emptyMap())
            loaded = true
        }
    }

    fun put(batch: List<IndexEntry>) {
        if (batch.isEmpty()) return
        runCatching {
            val w = db.writableDatabase
            w.beginTransaction()
            try {
                for (e in batch) {
                    val values = ContentValues(5).apply {
                        put(ColId, e.id)
                        put(ColModified, e.modified)
                        put(ColLabels, e.labels.joinToString(LabelSeparator))
                        put(ColText, e.text)
                        put(ColVersion, e.version)
                    }
                    w.insertWithOnConflict(Table, null, values, SQLiteDatabase.CONFLICT_REPLACE)
                }
                w.setTransactionSuccessful()
            } finally {
                w.endTransaction()
            }
        }
        _entries.update { current -> current + batch.associateBy { it.id } }
    }

    fun putVectors(batch: Map<Long, VectorEntry>) {
        if (batch.isEmpty()) return
        runCatching {
            val w = db.writableDatabase
            w.beginTransaction()
            try {
                for ((id, e) in batch) {
                    val values = ContentValues(4).apply {
                        put(ColId, id)
                        put(ColModified, e.modified)
                        put(ColVector, VectorMath.toBytes(e.vector))
                        put(ColVersion, e.version)
                    }
                    w.insertWithOnConflict(VectorTable, null, values, SQLiteDatabase.CONFLICT_REPLACE)
                }
                w.setTransactionSuccessful()
            } finally {
                w.endTransaction()
            }
        }
        _vectors.update { current -> current + batch }
    }

    fun remove(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        runCatching {
            val w = db.writableDatabase
            w.beginTransaction()
            try {
                for (id in ids) {
                    w.delete(Table, "$ColId = ?", arrayOf(id.toString()))
                    w.delete(VectorTable, "$ColId = ?", arrayOf(id.toString()))
                }
                w.setTransactionSuccessful()
            } finally {
                w.endTransaction()
            }
        }
        val gone = ids.toSet()
        _entries.update { current -> current - gone }
        _vectors.update { current -> current - gone }
    }

    fun setStatus(status: IndexStatus) {
        _status.value = status
    }

    private fun readAll(): Map<Long, IndexEntry> {
        val out = HashMap<Long, IndexEntry>()
        db.readableDatabase.query(Table, arrayOf(ColId, ColModified, ColLabels, ColText, ColVersion), null, null, null, null, null).use { c ->
            while (c.moveToNext()) {
                val labels = c.getString(2).orEmpty()
                val entry = IndexEntry(
                    id = c.getLong(0),
                    modified = c.getLong(1),
                    labels = if (labels.isEmpty()) emptyList() else labels.split(LabelSeparator),
                    text = c.getString(3).orEmpty(),
                    version = c.getInt(4)
                )
                out[entry.id] = entry
            }
        }
        return out
    }

    private fun readVectors(): Map<Long, VectorEntry> {
        val out = HashMap<Long, VectorEntry>()
        db.readableDatabase.query(VectorTable, arrayOf(ColId, ColModified, ColVector, ColVersion), null, null, null, null, null).use { c ->
            while (c.moveToNext()) {
                val bytes = c.getBlob(2) ?: continue
                out[c.getLong(0)] = VectorEntry(c.getLong(1), VectorMath.fromBytes(bytes), c.getInt(3))
            }
        }
        return out
    }

    private class Helper(context: Context) : SQLiteOpenHelper(context, "index.db", null, 2) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE $Table ($ColId INTEGER PRIMARY KEY, $ColModified INTEGER NOT NULL, " +
                    "$ColLabels TEXT NOT NULL, $ColText TEXT NOT NULL, $ColVersion INTEGER NOT NULL)"
            )
            createVectors(db)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // 1 → 2: etiketler korunur, vektör tablosu eklenir.
            if (oldVersion < 2) createVectors(db)
        }

        override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS $Table")
            db.execSQL("DROP TABLE IF EXISTS $VectorTable")
            onCreate(db)
        }

        private fun createVectors(db: SQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS $VectorTable ($ColId INTEGER PRIMARY KEY, $ColModified INTEGER NOT NULL, " +
                    "$ColVector BLOB NOT NULL, $ColVersion INTEGER NOT NULL)"
            )
        }
    }

    private companion object {
        const val Table = "media"
        const val VectorTable = "vectors"
        const val ColVector = "vec"
        const val ColId = "id"
        const val ColModified = "modified"
        const val ColLabels = "labels"
        const val ColText = "text"
        const val ColVersion = "version"
        const val LabelSeparator = "|"
    }
}
