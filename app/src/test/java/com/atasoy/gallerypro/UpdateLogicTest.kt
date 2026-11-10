package com.atasoy.gallerypro

import com.atasoy.gallerypro.data.update.UpdateLogic
import org.junit.Assert.*
import org.junit.Test

class UpdateLogicTest {
    @Test fun nameOnlyAcceptsMain() {
        assertEquals("0.9.4", UpdateLogic.versionOf("Gallerypro 0.9.4 (main)"))
        assertNull(UpdateLogic.versionOf("Gallerypro 9.1.0 (asama-test)"))
    }
    @Test fun compareVersions() {
        assertTrue(UpdateLogic.isNewer("0.9.3", "0.9.4"))
        assertTrue(UpdateLogic.isNewer("0.9.9", "0.10.0"))
        assertFalse(UpdateLogic.isNewer("0.9.4", "0.9.3"))
        assertFalse(UpdateLogic.isNewer("0.9.4", "0.9.4"))
        assertFalse(UpdateLogic.isNewer("0.9.4", "unknown"))
    }
    @Test fun onlyApprovedHttpsApk() {
        assertTrue(UpdateLogic.allowedUrl(UpdateLogic.ApkUrl))
        assertFalse(UpdateLogic.allowedUrl("https://evil.test/app.apk"))
        assertFalse(UpdateLogic.allowedUrl(UpdateLogic.ApkUrl+"?redirect=1"))
    }
    @Test fun shaRequired() {
        val hash="abcdef0123456789".repeat(4)
        assertEquals(hash,UpdateLogic.validDigest("sha256:"+hash))
        assertNull(UpdateLogic.validDigest(null))
        assertNull(UpdateLogic.validDigest("abc"))
    }
}
