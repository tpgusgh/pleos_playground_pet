package com.pleosplayground.drivequest.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FakeSignalSourceTest {
    private suspend fun kotlinx.coroutines.test.TestScope.collectDrive(
        source: FakeSignalSource, style: DriveStyle,
    ): List<Sample> {
        val got = mutableListOf<Sample>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { source.state.collect { got += it } }
        source.drive(style)
        job.cancel()
        return got
    }

    @Test
    fun initial_state_is_healthy() {
        val s = FakeSignalSource().state.value
        assertEquals(80, s.fuelPercent)
        assertEquals(true, s.tireOk)
        assertEquals(true, s.doorsClosed)
        assertEquals(true, s.windowsClosed)
        assertEquals(true, s.wiperOk)
        assertNull(s.brakeDecel)
    }

    @Test
    fun ux_defaults_to_restricted_and_follows_setUx() {
        val source = FakeSignalSource()
        assertEquals(UxState.RESTRICTED, source.ux.value)
        source.setUx(UxState.PARKED)
        assertEquals(UxState.PARKED, source.ux.value)
    }

    @Test
    fun preset_low_fuel_sets_fuel_10_and_keeps_rest_ok() {
        val source = FakeSignalSource()
        source.applyPreset(Preset.LOW_FUEL)
        val s = source.state.value
        assertEquals(10, s.fuelPercent)
        assertEquals(listOf(true, true, true, true), listOf(s.tireOk, s.doorsClosed, s.windowsClosed, s.wiperOk))
    }

    @Test
    fun preset_needs_care_marks_tire_doors_windows_bad() {
        val source = FakeSignalSource()
        source.applyPreset(Preset.NEEDS_CARE)
        val s = source.state.value
        assertEquals(55, s.fuelPercent)
        assertEquals(listOf(false, false, false), listOf(s.tireOk, s.doorsClosed, s.windowsClosed))
        assertEquals(true, s.wiperOk)
    }

    @Test
    fun preset_unsupported_nulls_everything() {
        val source = FakeSignalSource()
        source.applyPreset(Preset.UNSUPPORTED)
        assertEquals(source.state.value.copy(tMs = 0), Sample())
    }

    @Test
    fun preset_keeps_ux() {
        val source = FakeSignalSource()
        source.setUx(UxState.PARKED)
        source.applyPreset(Preset.NEEDS_CARE)
        assertEquals(UxState.PARKED, source.ux.value)
    }

    @Test
    fun set_fuel_updates_state_and_null_clears_it() {
        val source = FakeSignalSource()
        source.setFuel(33)
        assertEquals(33, source.state.value.fuelPercent)
        source.setFuel(null)
        assertNull(source.state.value.fuelPercent)
    }

    @Test
    fun smooth_drive_has_no_hard_brakes_but_some_brakes() = runTest {
        val got = collectDrive(FakeSignalSource(), DriveStyle.SMOOTH)
        assertEquals(0, got.count { (it.brakeDecel ?: 0f) > 4.0f })
        assertTrue(got.count { it.brakeDecel != null } >= 1)
    }

    @Test
    fun harsh_drive_has_at_least_two_hard_brakes_and_sharp_steers() = runTest {
        val got = collectDrive(FakeSignalSource(), DriveStyle.HARSH)
        assertTrue(got.count { (it.brakeDecel ?: 0f) > 4.0f } >= 2)
        assertTrue(got.count { (it.steeringRate ?: 0f) > 120f } >= 1)
    }

    @Test
    fun drive_ends_with_quiet_sample_and_keeps_fuel() = runTest {
        val source = FakeSignalSource()
        source.drive(DriveStyle.HARSH)
        val s = source.state.value
        assertNull(s.brakeDecel)
        assertNull(s.steeringRate)
        assertEquals(80, s.fuelPercent)
    }
}
