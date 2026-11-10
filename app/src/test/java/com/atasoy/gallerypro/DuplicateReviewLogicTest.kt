package com.atasoy.gallerypro

import com.atasoy.gallerypro.data.clip.VectorEntry
import com.atasoy.gallerypro.data.collections.DuplicateReviewLogic
import com.atasoy.gallerypro.data.media.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateReviewLogicTest {
    private fun photo(id: Long, w: Int = 4000, h: Int = 3000, bytes: Long = 600L,
                      modified: Long = 42L, favorite: Boolean = false, video: Boolean = false): MediaItem =
        MediaItem(id = id, isVideo = video, takenAtMillis = id * 1000L, durationMs = 0,
            mimeType = if (video) "video/mp4" else "image/jpeg", width = w, height = h,
            album = "Camera", isFavorite = favorite, sizeBytes = bytes, modifiedMillis = modified)

    private fun v(which: Int): FloatArray = FloatArray(512).also { it[which] = 1f }
    private fun vectors(vararg pairs: Pair<Long, FloatArray>): Map<Long, VectorEntry> =
        pairs.associate { (id, values) -> id to VectorEntry(42L, values) }

    @Test fun sameBytesWithoutClipMatchNeverSuggested() {
        val a = photo(1)
        val b = photo(2)
        assertTrue(DuplicateReviewLogic.groups(listOf(a,b), vectors(1L to v(0), 2L to v(1))).isEmpty())
    }

    @Test fun highestResolutionIsKeptAcrossExistingCandidateSets() {
        val large = photo(1, 4000, 3000, 600)
        val largeCopy = photo(2, 4000, 3000, 600)
        val small = photo(3, 2000, 1500, 200)
        val smallCopy = photo(4, 2000, 1500, 200)
        val result = DuplicateReviewLogic.groups(
            listOf(small, largeCopy, large, smallCopy),
            vectors(1L to v(0),2L to v(0),3L to v(0),4L to v(0))
        )
        assertEquals(1, result.size)
        assertEquals(1L, result.single().keep.id)
        assertEquals(setOf(2L,3L,4L),result.single().discard.map{it.id}.toSet())
    }

    @Test fun staleAndMissingVectorsDoNotEnterBatchDeletion() {
        val a=photo(1)
        val b=photo(2)
        val c=photo(3, modified=60)
        val result=DuplicateReviewLogic.groups(
            listOf(a,b,c),
            vectors(1L to v(0),2L to v(0),3L to v(0))
        )
        assertEquals(1,result.size)
        assertEquals(listOf(2L),result.single().discard.map { it.id })
    }

    @Test fun videoCandidatesAreNotSuggestedForPhotoMerge() {
        val a=photo(1, video=true)
        val b=photo(2, video=true)
        assertTrue(DuplicateReviewLogic.groups(listOf(a,b),
            vectors(1L to v(0),2L to v(0))).isEmpty())
    }

    @Test fun differentImagesSplitInsteadOfChainMatching() {
        val items=(1L..4L).map { photo(it) }
        val result=DuplicateReviewLogic.groups(items,
            vectors(1L to v(0),2L to v(0),3L to v(1),4L to v(1)))
        assertEquals(2,result.size)
        assertTrue(result.all { it.discard.size==1 })
    }
}
