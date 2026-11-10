package com.atasoy.gallerypro

import com.atasoy.gallerypro.data.media.AlbumCopyLogic
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumCopyLogicTest {
    @Test fun imageDirectories() {
        assertTrue(AlbumCopyLogic.supported("DCIM/Camera/", false))
        assertTrue(AlbumCopyLogic.supported("Pictures/Family/", false))
        assertFalse(AlbumCopyLogic.supported("Movies/Videos/", false))
        assertFalse(AlbumCopyLogic.supported("Download/", false))
    }
    @Test fun videoDirectories() {
        assertTrue(AlbumCopyLogic.supported("Movies/Videos/", true))
        assertTrue(AlbumCopyLogic.supported("DCIM/Camera/", true))
    }
    @Test fun rejectsTraversal() {
        assertFalse(AlbumCopyLogic.supported("Pictures/../Private/", false))
    }
}
