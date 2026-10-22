package com.pleosplayground.drivequest.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UxGateTest {
    @Test
    fun only_parked_can_play() {
        assertTrue(canPlay(UxState.PARKED))
        assertFalse(canPlay(UxState.RESTRICTED))
    }
}
