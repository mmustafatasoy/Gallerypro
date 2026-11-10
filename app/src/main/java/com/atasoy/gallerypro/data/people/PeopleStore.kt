package com.atasoy.gallerypro.data.people

import android.content.Context
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File

/** Yüz vektörleri sadece uygulamanın özel depolamasında kalır; cihaz dışına gönderilmez. */
class PeopleStore(context: Context) {
    private val storage = File(context.filesDir, "people-sface-fp32-v1.dat")
    private val prefs = context.getSharedPreferences("people_names_v1", Context.MODE_PRIVATE)
    private val magic = 0x50455031

    fun load(): List<FaceVector> = runCatching {
        if (!storage.exists()) return emptyList()
        DataInputStream(storage.inputStream().buffered()).use { reader ->
            require(reader.readInt() == magic)
            val n = reader.readInt()
            require(n in 0..150_000)
            List(n) {
                val photoId = reader.readLong()
                val faceIndex = reader.readInt()
                val modified = reader.readLong()
                val dimension = reader.readInt()
                require(dimension == PeopleLogic.Dimensions)
                val vec = FloatArray(dimension) { reader.readFloat() }
                FaceVector(photoId,faceIndex,modified,vec)
            }
        }
    }.getOrElse { emptyList() }

    fun save(data: List<FaceVector>) {
        storage.parentFile?.mkdirs()
        val part = File(storage.path + ".part")
        try {
            DataOutputStream(part.outputStream().buffered()).use { out ->
                out.writeInt(magic)
                out.writeInt(data.size)
                for (face in data) {
                    out.writeLong(face.photoId)
                    out.writeInt(face.faceIndex)
                    out.writeLong(face.modifiedMillis)
                    out.writeInt(face.vector.size)
                    for (value in face.vector) out.writeFloat(value)
                }
            }
            if (!part.renameTo(storage)) error("Yüz dizini kaydedilemedi")
        } finally {
            part.delete()
        }
    }

    fun name(key: String): String = prefs.getString(key, "") ?: ""
    fun setName(key: String, name: String) = prefs.edit().putString(key, name.trim().take(100)).apply()
}
