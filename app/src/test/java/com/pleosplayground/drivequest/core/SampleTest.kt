package com.pleosplayground.drivequest.core

import org.junit.Assert.assertNull
import org.junit.Test

class SampleTest {
    @Test
    fun default_sample_has_all_signals_unsupported() {
        val s = Sample()
        assertNull(s.brakeDecel)
        assertNull(s.fuelPercent)
        assertNull(s.tireOk)
    }

    @Test
    fun new_trip_fields_default_to_null() {
        val s = Sample()
        assertNull(s.rangeKm)
        assertNull(s.destinationName)
        assertNull(s.distanceToDestinationKm)
        assertNull(s.etaMinutes)
    }
}
