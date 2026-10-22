package com.pleosplayground.drivequest.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PetTest {
    @Test
    fun food_clamps_and_passes_null() {
        assertEquals(100, food(Sample(fuelPercent = 150)))
        assertEquals(0, food(Sample(fuelPercent = -5)))
        assertNull(food(Sample()))
    }

    @Test
    fun health_is_percent_of_known_items_that_are_ok() {
        assertEquals(100, health(Sample(tireOk = true, doorsClosed = true, windowsClosed = true, wiperOk = true)))
        assertEquals(50, health(Sample(tireOk = true, doorsClosed = true, windowsClosed = false, wiperOk = false)))
        assertEquals(100, health(Sample(tireOk = true)))
        assertEquals(0, health(Sample(wiperOk = false)))
        assertNull(health(Sample()))
    }

    @Test
    fun hard_brake_lowers_mood_by_5() = assertEquals(-5, moodDelta(Sample(brakeDecel = 5f)))

    @Test
    fun smooth_brake_raises_mood_by_1() = assertEquals(1, moodDelta(Sample(brakeDecel = 2f)))

    @Test
    fun sharp_steer_lowers_mood_by_3() = assertEquals(-3, moodDelta(Sample(steeringRate = 150f)))

    @Test
    fun smooth_steer_raises_mood_by_1() = assertEquals(1, moodDelta(Sample(steeringRate = 30f)))

    @Test
    fun brake_and_steer_in_one_sample_add_up() =
        assertEquals(-8, moodDelta(Sample(brakeDecel = 5f, steeringRate = 150f)))

    @Test
    fun nan_negative_zero_and_infinite_readings_are_ignored() {
        for (v in listOf(Float.NaN, -1f, 0f, Float.POSITIVE_INFINITY)) {
            assertEquals("brake $v", 0, moodDelta(Sample(brakeDecel = v)))
            assertEquals("steer $v", 0, moodDelta(Sample(steeringRate = v)))
        }
    }

    @Test
    fun all_null_sample_has_zero_delta() = assertEquals(0, moodDelta(Sample()))

    @Test
    fun face_hungry_when_food_below_20() {
        assertEquals(Face.HUNGRY, faceFor(19, 100, 100))
        assertEquals(Face.HAPPY, faceFor(20, 100, 100))
    }

    @Test
    fun face_sulky_when_health_below_50_or_mood_below_40() {
        assertEquals(Face.SULKY, faceFor(100, 49, 100))
        assertEquals(Face.SULKY, faceFor(100, 100, 39))
        assertEquals(Face.HAPPY, faceFor(100, 50, 40))
    }

    @Test
    fun hungry_beats_sulky() = assertEquals(Face.HUNGRY, faceFor(0, 0, 0))

    @Test
    fun face_with_all_gauges_hidden_uses_mood_only() {
        assertEquals(Face.HAPPY, faceFor(null, null, 80))
        assertEquals(Face.SULKY, faceFor(null, null, 10))
    }
}
