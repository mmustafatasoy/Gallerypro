package com.atasoy.gallerypro.data.updates

internal object UpdatePolicy {
    const val PackageName = "com.atasoy.gallerypro"
    const val ManifestUrl =
        "https://github.com/mmustafatasoy/Gallerypro/releases/download/son-surum/Gallerypro-update.json"
    const val ApkUrl =
        "https://github.com/mmustafatasoy/Gallerypro/releases/download/son-surum/Gallerypro.apk"

    fun isDigest(value: String) = value.length == 64 &&
        value.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }

    fun validUpgrade(
        pkg: String, current: Long, next: Long, digest: String,
        signer: String, installedSigner: String
    ): Boolean = pkg == PackageName && next > current && next > 0L &&
        isDigest(digest) && isDigest(signer) && signer.equals(installedSigner, true)
}
