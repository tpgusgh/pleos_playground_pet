package com.pleosplayground.drivequest.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProactiveTest {
    private fun trip(range: Float?, dest: String?, dist: Float?, eta: Int? = null) = TripInfo(range, dest, dist, eta)
    private val shortTrip = trip(40f, "집", 120f, 100)

    @Test fun short_fires_once_and_disarms() {
        val d = decideAlert(AlertState(), shortTrip, 1_000, true)
        assertEquals(Alert.FUEL_SHORT, d.alert)
        assertFalse(d.state.shortArmed)
        assertFalse(d.state.lowArmed)
        assertEquals(1_000L, d.state.lastSpokeMs)
        assertNull(decideAlert(d.state, shortTrip, 1_000 + 400_000, true).alert)
    }

    @Test fun priority_short_over_low_over_near() {
        assertEquals(Alert.FUEL_SHORT, decideAlert(AlertState(), trip(40f, "집", 120f), 0, true).alert)
        assertEquals(Alert.LOW_RANGE, decideAlert(AlertState(), trip(40f, null, null), 0, true).alert)
        assertEquals(Alert.NEARLY_THERE, decideAlert(AlertState(), trip(500f, "집", 0.8f), 0, true).alert)
    }

    @Test fun gap_blocks_and_alert_waits() {
        val first = decideAlert(AlertState(), shortTrip, 0, true)
        val near = trip(500f, "집", 0.8f)
        val blocked = decideAlert(first.state, near, 60_000, true)
        assertNull(blocked.alert)
        assertTrue(blocked.state.nearArmed)
        assertNull(decideAlert(blocked.state, near, 180_000 - 1, true).alert)
        assertEquals(Alert.NEARLY_THERE, decideAlert(blocked.state, near, 180_000, true).alert)
    }

    @Test fun alert_is_not_blocked_when_the_clock_went_backwards() {
        val d = decideAlert(AlertState(lastSpokeMs = 10_000_000), shortTrip, 5_000_000, true)
        assertEquals(Alert.FUEL_SHORT, d.alert)
        assertEquals(5_000_000L, d.state.lastSpokeMs)
    }

    @Test fun cannot_speak_changes_nothing_but_rearming() {
        val d = decideAlert(AlertState(), shortTrip, 0, false)
        assertNull(d.alert)
        assertTrue(d.state.shortArmed && d.state.lowArmed && d.state.nearArmed)
        assertNull(d.state.lastSpokeMs)
    }

    @Test fun rearm_after_recovery() {
        val first = decideAlert(AlertState(), shortTrip, 0, true)
        val ok = decideAlert(first.state, trip(500f, "집", 100f), 10_000, true)
        assertNull(ok.alert)
        assertTrue(ok.state.shortArmed && ok.state.lowArmed)
        assertEquals(Alert.FUEL_SHORT, decideAlert(ok.state, shortTrip, 200_000, true).alert)
    }

    @Test fun rearm_on_new_destination() {
        val first = decideAlert(AlertState(), shortTrip, 0, true)
        assertEquals(Alert.FUEL_SHORT, decideAlert(first.state, trip(40f, "회사", 120f), 200_000, true).alert)
    }

    @Test fun unknown_and_invalid_values_never_alert() {
        listOf(
            trip(null, null, null),
            trip(Float.NaN, "집", Float.NaN),
            trip(-5f, "집", -5f),
            trip(Float.POSITIVE_INFINITY, "집", Float.NEGATIVE_INFINITY),
        ).forEach { assertNull(decideAlert(AlertState(), it, 0, true).alert) }
    }

    @Test fun near_requires_a_destination() {
        assertNull(decideAlert(AlertState(), trip(500f, null, 0.5f), 0, true).alert)
    }

    @Test fun spoke_marks_the_time_only() {
        assertEquals(AlertState(lastSpokeMs = 5_000), AlertState().spoke(5_000))
    }

    @Test fun alert_lines() {
        assertEquals("목적지까지 연료가 모자랄 것 같아요", alertLine(Alert.FUEL_SHORT))
        assertEquals("연료가 얼마 안 남았어요", alertLine(Alert.LOW_RANGE))
        assertEquals("거의 다 왔어요!", alertLine(Alert.NEARLY_THERE))
    }
}
