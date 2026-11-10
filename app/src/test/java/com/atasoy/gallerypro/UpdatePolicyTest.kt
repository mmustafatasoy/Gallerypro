package com.atasoy.gallerypro

import com.atasoy.gallerypro.data.updates.UpdatePolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePolicyTest {
    private val a = "a".repeat(64)
    private val b = "b".repeat(64)

    @Test fun newerVersionsNeedMatchingSignatures() {
        assertTrue(UpdatePolicy.validUpgrade("com.atasoy.gallerypro", 25, 26, a, b, b))
        assertFalse(UpdatePolicy.validUpgrade("com.atasoy.gallerypro", 26, 26, a, b, b))
        assertFalse(UpdatePolicy.validUpgrade("com.atasoy.gallerypro", 27, 26, a, b, b))
        assertFalse(UpdatePolicy.validUpgrade("com.atasoy.gallerypro", 25, 26, a, b, a))
    }

    @Test fun untrustedPackageOrHashRejected() {
        assertFalse(UpdatePolicy.validUpgrade("com.fake", 25, 26, a, b, b))
        assertFalse(UpdatePolicy.validUpgrade("com.atasoy.gallerypro", 25, 26, "bad", b, b))
        assertFalse(UpdatePolicy.isDigest("g".repeat(64)))
        assertTrue(UpdatePolicy.isDigest("F".repeat(64)))
    }
}
