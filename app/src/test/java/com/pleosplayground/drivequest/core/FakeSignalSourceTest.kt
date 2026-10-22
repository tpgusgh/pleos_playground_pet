package com.pleosplayground.drivequest.core

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeSignalSourceTest {
    private suspend fun samples(s: Scenario) = FakeSignalSource(s).trip().toList()

    @Test
    fun smooth_has_no_hard_brakes() = runTest {
        assertEquals(0, samples(Scenario.SMOOTH).count { (it.brakeDecel ?: 0f) > 4.0f })
    }

    @Test
    fun aggressive_has_at_least_two_hard_brakes() = runTest {
        assertTrue(samples(Scenario.AGGRESSIVE).count { (it.brakeDecel ?: 0f) > 4.0f } >= 2)
    }

    @Test
    fun unsupported_has_all_null_signals() = runTest {
        val list = samples(Scenario.UNSUPPORTED)
        assertTrue(list.isNotEmpty())
        assertTrue(list.all { it == Sample(tMs = it.tMs) })
    }

    @Test
    fun rainy_night_sets_wiper_and_headlight() = runTest {
        val list = samples(Scenario.RAINY_NIGHT)
        assertTrue(list.any { it.wiperOn == true })
        assertTrue(list.any { it.headlightOn == true })
    }

    @Test
    fun ux_defaults_to_restricted_and_follows_setUx() {
        val source = FakeSignalSource(Scenario.SMOOTH)
        assertEquals(UxState.RESTRICTED, source.ux.value)
        source.setUx(UxState.PARKED)
        assertEquals(UxState.PARKED, source.ux.value)
    }

    @Test
    fun odometer_is_non_decreasing_and_positive_at_end() = runTest {
        for (s in listOf(Scenario.SMOOTH, Scenario.AGGRESSIVE, Scenario.RAINY_NIGHT)) {
            val km = samples(s).mapNotNull { it.odometerKm }
            assertEquals(km.sorted(), km)
            assertTrue(km.last() > 0f)
        }
    }
}
