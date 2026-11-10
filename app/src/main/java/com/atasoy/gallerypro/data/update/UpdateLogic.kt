package com.atasoy.gallerypro.data.update

/** Github sürüm adını ve güvenli dosya bilgisini doğrulayan saf Kotlin kuralları. */
object UpdateLogic {
    const val ApkUrl = "https://github.com/mmustafatasoy/Gallerypro/releases/download/son-surum/Gallerypro.apk"
    private val Release = Regex("""^Gallerypro\s+(\d+\.\d+\.\d+)\s+\(main\)$""")
    fun versionOf(name: String): String? = Release.matchEntire(name.trim())?.groupValues?.get(1)
    fun isNewer(current: String, remote: String): Boolean {
        fun parse(s: String): List<Int>? {
            val pieces = s.split('.')
            if (pieces.size != 3) return null
            return pieces.map { it.toIntOrNull() ?: return null }
        }
        val a = parse(current) ?: return false
        val b = parse(remote) ?: return false
        return (0..2).firstOrNull { a[it] != b[it] }?.let { b[it] > a[it] } ?: false
    }
    fun allowedUrl(value: String): Boolean = value == ApkUrl
    fun validDigest(digest: String?): String? {
        val text = digest?.removePrefix("sha256:") ?: return null
        return text.takeIf { it.length == 64 && it.all { c ->
            c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F'
        } }?.lowercase()
    }
}
