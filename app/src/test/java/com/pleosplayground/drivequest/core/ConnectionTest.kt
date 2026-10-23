package com.pleosplayground.drivequest.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConnectionTest {
    @Test
    fun connection_labels() {
        assertEquals("차량과 연결하는 중이에요", connectionLabel(ConnectionState.CONNECTING))
        assertEquals("차량과 연결되지 않았어요", connectionLabel(ConnectionState.UNAVAILABLE))
        assertNull(connectionLabel(ConnectionState.CONNECTED))
    }

    @Test
    fun unavailable_source_counts_as_driving_with_no_signals() {
        val s = UnavailableSignalSource()
        assertEquals(ConnectionState.UNAVAILABLE, s.connection.value)
        assertEquals(UxState.RESTRICTED, s.ux.value)
        assertEquals(Sample(), s.state.value)
        assertNull(food(s.state.value))
        assertNull(health(s.state.value))
        assertEquals(TripInfo(null, null, null, null), tripInfo(s.state.value))
        assertEquals(0, moodDelta(s.state.value))
    }
}
