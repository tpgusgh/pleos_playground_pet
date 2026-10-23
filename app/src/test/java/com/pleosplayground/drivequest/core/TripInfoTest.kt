package com.pleosplayground.drivequest.core

import org.junit.Assert.assertEquals
import org.junit.Test

class TripInfoTest {
    @Test
    fun trip_info_copies_the_four_values() {
        val s = Sample(rangeKm = 120f, destinationName = "회사", distanceToDestinationKm = 35f, etaMinutes = 40)
        assertEquals(TripInfo(120f, "회사", 35f, 40), tripInfo(s))
        assertEquals(TripInfo(null, null, null, null), tripInfo(Sample()))
    }

    @Test
    fun fuel_verdict_unknown_when_a_value_is_missing_or_invalid() {
        val inf = Float.POSITIVE_INFINITY
        listOf(
            null to 10f, 10f to null, Float.NaN to 10f, 10f to Float.NaN,
            -1f to 10f, 10f to -1f, inf to 10f, 10f to inf,
        ).forEach { (r, d) -> assertEquals("$r,$d", FuelVerdict.UNKNOWN, fuelVerdict(r, d)) }
    }

    @Test
    fun fuel_verdict_boundaries() {
        assertEquals(FuelVerdict.TIGHT, fuelVerdict(100f, 100f))
        assertEquals(FuelVerdict.ENOUGH, fuelVerdict(115f, 100f))
        assertEquals(FuelVerdict.TIGHT, fuelVerdict(114.9f, 100f))
        assertEquals(FuelVerdict.SHORT, fuelVerdict(99.9f, 100f))
    }

    @Test
    fun fuel_verdict_with_zero_distance() {
        assertEquals(FuelVerdict.TIGHT, fuelVerdict(0f, 0f))
        assertEquals(FuelVerdict.ENOUGH, fuelVerdict(5f, 0f))
    }
}
