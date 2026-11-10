package com.atasoy.gallerypro

import com.atasoy.gallerypro.data.edit.MetadataEditLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class MetadataEditLogicTest {
    @Test fun onlyJpegIsWritable() {
        assertTrue(MetadataEditLogic.isSupported("image/jpeg", false))
        assertTrue(MetadataEditLogic.isSupported("IMAGE/JPEG", false))
        assertFalse(MetadataEditLogic.isSupported("image/heic", false))
        assertFalse(MetadataEditLogic.isSupported("image/png", false))
        assertFalse(MetadataEditLogic.isSupported("image/jpeg", true))
    }

    @Test fun validDateAndGps() {
        val value = MetadataEditLogic.parse("2026-11-10 15:30", "36,90", "30.73", false)!!
        assertEquals("2026:11:10 15:30:00", value.exifDateTime())
        assertEquals(36.9, value.latitude!!, 0.000001)
        assertEquals(30.73, value.longitude!!, 0.000001)
        assertEquals("2026-11-10 15:30",
            MetadataEditLogic.formatDate(value.epochMillis(ZoneId.of("Europe/Istanbul")),
                ZoneId.of("Europe/Istanbul")))
    }

    @Test fun blankCoordinatesDoNotEraseExistingGps() {
        val value = MetadataEditLogic.parse("2025-01-09 02:03", "", "", false)!!
        assertNull(value.latitude)
        assertNull(value.longitude)
        assertFalse(value.removeLocation)
    }

    @Test fun explicitRemoveGpsWorks() {
        val value = MetadataEditLogic.parse("2025-01-09 02:03", "", "", true)!!
        assertTrue(value.removeLocation)
    }

    @Test fun gpsDmsIsCorrectForSignedCoordinates() {
        assertEquals("36/1,54/1,0/10000", MetadataEditLogic.toGpsDms(36.9))
        assertEquals("30/1,43/1,480000/10000", MetadataEditLogic.toGpsDms(-30.73))
        assertEquals("0/1,0/1,0/10000", MetadataEditLogic.toGpsDms(0.0))
    }

    @Test fun invalidValuesAreRejectedWithoutWrite() {
        assertNull(MetadataEditLogic.parse("2026-02-30 12:00", "", "", false))
        assertNull(MetadataEditLogic.parse("2026-11-10 25:00", "", "", false))
        assertNull(MetadataEditLogic.parse("2026-11-10 12:00", "91", "0", false))
        assertNull(MetadataEditLogic.parse("2026-11-10 12:00", "-90", "181", false))
        assertNull(MetadataEditLogic.parse("2026-11-10 12:00", "2", "", false))
        assertNull(MetadataEditLogic.parse("2026-11-10 12:00", "NaN", "0", false))
    }
}
