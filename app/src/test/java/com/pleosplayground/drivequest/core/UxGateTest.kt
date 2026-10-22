package com.pleosplayground.drivequest.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UxGateTest {
    private fun ts(xp: Int) = TripScore(xp, 0, 0f, 0, 0, 0, 0, emptySet())

    @Test
    fun only_parked_can_play() {
        assertTrue(canPlay(UxState.PARKED))
        assertFalse(canPlay(UxState.RESTRICTED))
    }

    @Test
    fun summary_mentions_levelup() =
        assertTrue("레벨업" in summaryLine(ts(130), leveledUp = true))

    @Test
    fun summary_shows_xp_otherwise() =
        assertTrue("경험치 42" in summaryLine(ts(42), leveledUp = false))

    @Test
    fun summary_for_empty_trip() =
        assertTrue("얻은 경험치가 없어요" in summaryLine(score(emptyList()), leveledUp = false))
}
