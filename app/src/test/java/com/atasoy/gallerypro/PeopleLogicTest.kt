package com.atasoy.gallerypro

import com.atasoy.gallerypro.data.people.FaceVector
import com.atasoy.gallerypro.data.people.PeopleLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PeopleLogicTest {
    private fun unit(index: Int): FloatArray = FloatArray(128).apply { this[index] = 1f }
    private fun face(photo: Long, faceIndex: Int, vec: FloatArray) =
        FaceVector(photo, faceIndex, 1000L, vec)

    @Test fun sameFaceAcrossPhotosGroupsTogether() {
        val groups = PeopleLogic.groups(listOf(face(2,0,unit(0)),face(1,0,unit(0)),face(3,0,unit(1))))
        assertEquals(2,groups.size)
        assertEquals(listOf(1L,2L),groups[0].photos)
        assertEquals("1:0",groups[0].key)
    }

    @Test fun differentFacesInSamePhotoNeverMerge() {
        val faces = listOf(face(1,0,unit(0)),face(1,1,unit(0)))
        assertEquals(2,PeopleLogic.groups(faces).size)
    }

    @Test fun invalidOrZeroEmbeddingsAreExcluded() {
        assertNull(PeopleLogic.normalized(FloatArray(128)))
        assertNull(PeopleLogic.normalized(floatArrayOf(1f)))
        assertNull(PeopleLogic.normalized(FloatArray(128).apply { this[0]=Float.NaN }))
        assertTrue(PeopleLogic.groups(listOf(face(2,0,FloatArray(128)))).isEmpty())
    }

    @Test fun ambiguousOrOppositeFacesRemainSeparate() {
        val a=unit(0)
        val b=unit(1)
        assertEquals(2,PeopleLogic.groups(listOf(face(1,0,a),face(2,0,b))).size)
        assertEquals(-1f,PeopleLogic.cosine(a, floatArrayOf(1f)),0f)
    }
}
