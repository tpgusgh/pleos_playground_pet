package com.pleosplayground.drivequest.ui

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

class RoomSceneTest {
    @Test fun hearts_null_stays_null() = assertNull(heartsFor(null))

    @Test fun hearts_round_up_in_quarters() {
        val expected = mapOf(0 to 0, 1 to 1, 25 to 1, 26 to 2, 50 to 2, 51 to 3, 75 to 3, 76 to 4, 100 to 4)
        expected.forEach { (v, h) -> assertEquals("value=$v", h, heartsFor(v)!!) }
    }

    @Test fun hearts_clamp_out_of_range() {
        assertEquals(0, heartsFor(-5)!!)
        assertEquals(4, heartsFor(150)!!)
    }
}
