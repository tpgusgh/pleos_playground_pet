package com.pleosplayground.drivequest.core

import org.junit.Assert.assertEquals
import org.junit.Test

class HeroTest {
    private fun ts(xp: Int, smooth: Int = 0, combo: Int = 0) =
        TripScore(xp, 0, 0f, 0, smooth, 0, combo, emptySet())

    @Test
    fun gain_below_threshold_keeps_level() {
        val h = Hero().gain(ts(99))
        assertEquals(1, h.level)
        assertEquals(99, h.xp)
    }

    @Test
    fun gain_levels_up_and_carries_over() =
        assertEquals(Hero(2, 30, 110, 12, 6, 6), Hero().gain(ts(130)))

    @Test
    fun gain_can_skip_multiple_levels() =
        assertEquals(Hero(3, 0, 120, 14, 7, 7), Hero().gain(ts(300)))

    @Test
    fun trip_bonus_adds_def_and_agi() {
        val h = Hero().gain(ts(0, smooth = 20, combo = 6))
        assertEquals(7, h.def)
        assertEquals(7, h.agi)
    }

    @Test
    fun encode_decode_roundtrip() {
        val h = Hero(3, 40, 120, 14, 7, 7)
        assertEquals(h, decodeHero(h.encode()))
    }

    @Test
    fun decode_garbage_returns_default() {
        for (raw in listOf("abc", "", null, "1,2,3", "0,0,100,10,5,5", "-1,0,100,10,5,5")) {
            assertEquals(Hero(), decodeHero(raw))
        }
    }

    @Test
    fun decode_rejects_zero_hp() = assertEquals(Hero(), decodeHero("3,40,0,14,7,7"))
}
