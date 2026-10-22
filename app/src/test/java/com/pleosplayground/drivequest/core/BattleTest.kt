package com.pleosplayground.drivequest.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BattleTest {
    @Test
    fun fresh_hero_beats_slime() =
        assertTrue(fight(Hero(), Monster("슬라임", 30, 5, 0, 1), 1L).won)

    @Test
    fun fresh_hero_loses_to_week0_boss() =
        assertFalse(fight(Hero(), weeklyBoss(0), 1L).won)

    @Test
    fun boosted_hero_beats_week0_boss() {
        val hero = bossChallenger(Hero(5, 0, 140, 18, 9, 9), weekXp = 400)
        assertTrue(fight(hero, weeklyBoss(0), 1L).won)
    }

    @Test
    fun same_seed_same_result() {
        val a = fight(Hero(), weeklyBoss(0), 7L)
        assertEquals(a, fight(Hero(), weeklyBoss(0), 7L))
    }

    @Test
    fun dead_hero_loses_immediately() {
        val r = fight(Hero(hp = 0), Monster("슬라임", 30, 5, 0, 1), 1L)
        assertFalse(r.won)
        assertEquals(0, r.rounds)
    }

    @Test
    fun stalemate_is_a_loss_after_50_rounds() {
        val r = fight(Hero(), Monster("벽", 100000, 0, 100, 0), 1L)
        assertFalse(r.won)
        assertEquals(50, r.rounds)
    }

    @Test
    fun weekly_boss_scales_with_week() {
        val b = weeklyBoss(2)
        assertEquals(listOf(300, 16, 8, 6), listOf(b.hp, b.atk, b.def, b.agi))
    }

    @Test
    fun boss_challenger_adds_week_xp_to_atk() =
        assertEquals(18, bossChallenger(Hero(), weekXp = 400).atk)
}
