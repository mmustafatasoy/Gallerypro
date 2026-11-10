package com.atasoy.gallerypro.data.index

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.atasoy.gallerypro.data.clip.VectorEntry
import com.atasoy.gallerypro.data.clip.VectorMath
import com.atasoy.gallerypro.data.places.PlaceEntry
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

    /** Çekildiği yer (öğe kimliği → şehir/il/ülke). Konumu olmayanlar da boş kayıtla durur. */
    private val _places = MutableStateFlow<Map<Long, PlaceEntry>>(emptyMap())
    val places: StateFlow<Map<Long, PlaceEntry>> = _places.asStateFlow()

    private val _status = MutableStateFlow(IndexStatus())
    val status: StateFlow<IndexStatus> = _status.asStateFlow()

    /** İlk okumada diskten yükler. Ana iş parçacığında çağrılmaz. */
    fun ensureLoaded() {
        if (loaded) return
        synchronized(lock) {
            if (loaded) return
            _entries.value = runCatching { readAll() }.getOrDefault(emptyMap())
            _vectors.value = runCatching { readVectors() }.getOrDefault(emptyMap())
            _places.value = runCatching { readPlaces() }.getOrDefault(emptyMap())
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

    fun putPlaces(batch: Map<Long, PlaceEntry>) {
        if (batch.isEmpty()) return
        runCatching {
            val w = db.writableDatabase
            w.beginTransaction()
            try {
                for ((id, p) in batch) {
                    val values = ContentValues(8).apply {
                        put(ColId, id)
                        put(ColModified, p.modified)
                        if (p.lat != null) put(ColLat, p.lat) else putNull(ColLat)
                        if (p.lon != null) put(ColLon, p.lon) else putNull(ColLon)
                        put(ColCity, p.city)
                        put(ColAdmin, p.admin)
                        put(ColCountry, p.country)
                        put(ColVersion, p.version)
                    }
                    w.insertWithOnConflict(PlaceTable, null, values, SQLiteDatabase.CONFLICT_REPLACE)
                }
                w.setTransactionSuccessful()
            } finally {
                w.endTransaction()
            }
        }
        _places.update { current -> current + batch }
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
                    w.delete(PlaceTable, "$ColId = ?", arrayOf(id.toString()))
                }
                w.setTransactionSuccessful()
            } finally {
                w.endTransaction()
            }
        }
        val gone = ids.toSet()
        _entries.update { current -> current - gone }
        _vectors.update { current -> current - gone }
        _places.update { current -> current - gone }
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

    private fun readPlaces(): Map<Long, PlaceEntry> {
        val out = HashMap<Long, PlaceEntry>()
        db.readableDatabase.query(
            PlaceTable,
            arrayOf(ColId, ColModified, ColLat, ColLon, ColCity, ColAdmin, ColCountry, ColVersion),
            null, null, null, null, null
        ).use { c ->
            while (c.moveToNext()) {
                out[c.getLong(0)] = PlaceEntry(
                    modified = c.getLong(1),
                    lat = if (c.isNull(2)) null else c.getDouble(2),
                    lon = if (c.isNull(3)) null else c.getDouble(3),
                    city = c.getString(4).orEmpty(),
                    admin = c.getString(5).orEmpty(),
                    country = c.getString(6).orEmpty(),
                    version = c.getInt(7)
                )
            }
        }
        return out
    }

    private class Helper(context: Context) : SQLiteOpenHelper(context, "index.db", null, 3) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE $Table ($ColId INTEGER PRIMARY KEY, $ColModified INTEGER NOT NULL, " +
                    "$ColLabels TEXT NOT NULL, $ColText TEXT NOT NULL, $ColVersion INTEGER NOT NULL)"
            )
            createVectors(db)
            createPlaces(db)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // 1 → 2: etiketler korunur, vektör tablosu eklenir. 2 → 3: yer tablosu eklenir.
            if (oldVersion < 2) createVectors(db)
            if (oldVersion < 3) createPlaces(db)
        }

        override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS $Table")
            db.execSQL("DROP TABLE IF EXISTS $VectorTable")
            db.execSQL("DROP TABLE IF EXISTS $PlaceTable")
            onCreate(db)
        }

        private fun createPlaces(db: SQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS $PlaceTable ($ColId INTEGER PRIMARY KEY, $ColModified INTEGER NOT NULL, " +
                    "$ColLat REAL, $ColLon REAL, $ColCity TEXT NOT NULL, $ColAdmin TEXT NOT NULL, " +
                    "$ColCountry TEXT NOT NULL, $ColVersion INTEGER NOT NULL)"
            )
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
        const val PlaceTable = "places"
        const val ColLat = "lat"
        const val ColLon = "lon"
        const val ColCity = "city"
        const val ColAdmin = "admin"
        const val ColCountry = "country"
        const val ColId = "id"
        const val ColModified = "modified"
        const val ColLabels = "labels"
        const val ColText = "text"
        const val ColVersion = "version"
        const val LabelSeparator = "|"
    }
}
