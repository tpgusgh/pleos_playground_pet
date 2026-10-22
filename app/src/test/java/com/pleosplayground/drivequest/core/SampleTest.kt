package com.pleosplayground.drivequest.core

import org.junit.Assert.assertNull
import org.junit.Test

class SampleTest {
    @Test
    fun default_sample_has_all_signals_unsupported() {
        val s = Sample(tMs = 0)
        assertNull(s.brakeDecel)
        assertNull(s.odometerKm)
        assertNull(s.beltOn)
    }
}
