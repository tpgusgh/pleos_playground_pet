package com.pleosplayground.drivequest.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PetFaceTimingTest {
    @Test fun eyes_are_open_outside_blinks() {
        assertEquals(1f, eyeOpenness(0), 0f)
        assertEquals(1f, eyeOpenness(1000), 0f)
        assertEquals(1f, eyeOpenness(5000), 0f)
    }

    @Test fun blink_closes_then_reopens() {
        assertEquals(1f, eyeOpenness(2600), 0.001f)
        assertEquals(0.5f, eyeOpenness(2645), 0.001f)
        assertEquals(0f, eyeOpenness(2690), 0.001f)
        assertEquals(0.5f, eyeOpenness(2735), 0.001f)
    }

    @Test fun blinks_repeat_every_loop_and_handle_negative_time() {
        assertEquals(0f, eyeOpenness(2690L + LoopMs), 0.001f)
        assertEquals(0f, eyeOpenness(2690L + 7L * LoopMs), 0.001f)
        assertEquals(0f, eyeOpenness(2690L - LoopMs), 0.001f)
    }

    @Test fun four_closures_per_loop_including_a_double_blink() {
        val values = (0 until LoopMs).map { eyeOpenness(it.toLong()) }
        assertTrue(values.all { it in 0f..1f })
        assertEquals(4, values.count { it == 0f })
    }

    @Test fun ear_twitch_is_zero_at_rest_and_bounded() {
        assertEquals(0f, earTwitch(0), 0f)
        assertEquals(0f, earTwitch(4000), 0f)
        val values = (0 until LoopMs).map { earTwitch(it.toLong()) }
        assertTrue(values.all { abs(it) <= 8f })
        assertTrue(values.any { abs(it) > 3f })
        assertEquals(earTwitch(1300), earTwitch(1300L + LoopMs), 0f)
    }
}
