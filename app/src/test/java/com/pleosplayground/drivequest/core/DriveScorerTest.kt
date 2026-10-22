package com.pleosplayground.drivequest.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveScorerTest {
    private val zero = TripScore(0, 0, 0f, 0, 0, 0, 0, emptySet())

    private fun odo(vararg km: Float) = km.mapIndexed { i, v -> Sample(i * 1000L, odometerKm = v) }

    private fun brakes(vararg d: Float) = d.mapIndexed { i, v -> Sample(i * 1000L, brakeDecel = v) }

    @Test
    fun empty_trip_scores_zero() = assertEquals(zero, score(emptyList()))

    @Test
    fun all_null_signals_score_zero() = assertEquals(zero, score(List(5) { Sample(it * 1000L) }))

    @Test
    fun distance_xp_is_10_per_km_capped_at_200() {
        val a = score(odo(0f, 10f))
        assertEquals(100, a.xp)
        assertEquals(10f, a.km, 0.001f)
        val b = score(odo(0f, 50f))
        assertEquals(200, b.xp)
        assertEquals(50f, b.km, 0.001f)
    }

    @Test
    fun odometer_decrease_is_ignored() {
        val s = score(odo(5f, 8f, 2f, 4f))
        assertEquals(5f, s.km, 0.001f)
        assertEquals(50, s.xp)
    }

    @Test
    fun belt_on_gives_20() = assertEquals(20, score(listOf(Sample(0, beltOn = true))).xp)

    @Test
    fun hard_brake_costs_5_smooth_brake_gives_1() {
        val s = score(brakes(5f, 5f, 2f, 2f, 2f))
        assertEquals(2, s.hardBrakes)
        assertEquals(3, s.smoothBrakes)
        assertEquals(0, s.xp) // -10 + 3 floors at 0
    }

    @Test
    fun smooth_brake_xp_adds_when_not_floored() {
        val s = score(odo(0f, 1f) + brakes(2f, 2f))
        assertEquals(12, s.xp) // 10 distance + 2 smooth
    }

    @Test
    fun nan_and_negative_decel_are_ignored() {
        val s = score(brakes(Float.NaN, -1f))
        assertEquals(0, s.hardBrakes)
        assertEquals(0, s.smoothBrakes)
    }

    @Test
    fun sharp_steering_is_counted() {
        val s = score(listOf(Sample(0, steeringRate = 150f), Sample(1000, steeringRate = 30f)))
        assertEquals(1, s.sharpSteers)
    }

    @Test
    fun signaled_turns_build_combo_and_unsignaled_resets() {
        val seq = listOf(
            true to true, false to false, true to true, false to false, true to true, false to false,
            true to false, false to false, true to true,
        )
        val s = score(seq.mapIndexed { i, (turning, signal) ->
            Sample(i * 1000L, turning = turning, turnSignalOn = signal)
        })
        assertEquals(3, s.bestCombo)
        assertEquals(8, s.xp) // 4 signaled turns x 2
    }

    @Test
    fun eco_multiplies_by_1_2() {
        val s = score(listOf(Sample(0, odometerKm = 0f, ecoMode = true), Sample(1000, odometerKm = 10f)))
        assertEquals(120, s.xp)
        assertTrue(Flag.ECO in s.flags)
    }

    @Test
    fun wiper_and_headlight_set_flags() {
        assertEquals(setOf(Flag.STORM, Flag.NIGHT), score(listOf(Sample(0, wiperOn = true, headlightOn = true))).flags)
    }

    @Test
    fun tire_ok_adds_5_gold() {
        val s = score(listOf(Sample(0, tireOk = true)))
        assertEquals(0, s.xp)
        assertEquals(5, s.gold)
    }
}
