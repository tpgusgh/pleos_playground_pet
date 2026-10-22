package com.pleosplayground.drivequest.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PetStateTest {
    private val hour = 3_600_000L
    private val min = 60_000L

    @Test fun first_run_does_not_decay() {
        val s = PetState().settle(10_000_000_000)
        assertEquals(70, s.mood)
        assertEquals(10_000_000_000, s.settledAtMs)
    }

    @Test fun decay_is_1_per_30_minutes_with_floor() {
        assertEquals(62, PetState(70, 1_000).settle(1_000 + 4 * hour).mood)
        assertEquals(0, PetState(5, 1_000).settle(1_000 + 24 * hour).mood)
    }

    @Test fun clock_going_backwards_does_not_decay() {
        assertEquals(70, PetState(70, 5_000_000).settle(1_000).mood)
    }

    @Test fun frequent_settles_do_not_lose_decay() {
        val a = PetState(70, 1_000).settle(1_000 + 29 * min)
        assertEquals(70, a.mood)
        assertEquals(68, a.settle(1_000 + 60 * min).mood)
    }

    @Test fun driving_delta_is_clamped_to_0_100() {
        assertEquals(100, PetState(98, 1_000).applyDriving(5, 1_000).mood)
        assertEquals(0, PetState(3, 1_000).applyDriving(-5, 1_000).mood)
    }

    @Test fun pet_adds_10_and_sets_cooldown() {
        val s = PetState(50, 1_000).pet(2_000)
        assertEquals(60, s.mood)
        assertEquals(7_000, s.petReadyAtMs)
    }

    @Test fun pet_inside_cooldown_changes_nothing() {
        val s = PetState(50, 1_000).pet(2_000)
        assertEquals(s, s.pet(3_000))
        assertEquals(70, s.pet(7_000).mood)
    }

    @Test fun talk_adds_5_and_replies_by_face() {
        val r = PetState(50, 1_000).talk(2_000, Face.HUNGRY)
        assertEquals(55, r.state.mood)
        assertEquals(greeting(Face.HUNGRY), r.reply)
    }

    @Test fun talk_inside_cooldown_has_no_reply_and_no_change() {
        val r = PetState(50, 1_000).talk(2_000, Face.HAPPY)
        val r2 = r.state.talk(3_000, Face.HAPPY)
        assertNull(r2.reply)
        assertEquals(r.state, r2.state)
    }

    @Test fun lines_differ_by_face_and_are_never_empty() {
        for (f in listOf(::greeting, ::driveStartLine)) {
            val lines = Face.values().map { f(it) }
            assertTrue(lines.all { it.isNotEmpty() })
            assertEquals(3, lines.toSet().size)
        }
        assertNotEquals(greeting(Face.HAPPY), driveStartLine(Face.HAPPY))
    }

    @Test fun encode_decode_roundtrip() {
        val s = PetState(63, 123_456, 7_000, 9_000)
        assertEquals(s, decodePet(s.encode()))
    }

    @Test fun decode_garbage_returns_default() {
        for (raw in listOf("abc", "", null, "1,2,3", "101,0,0,0", "-1,0,0,0", "50,-5,0,0", "50,x,0,0")) {
            assertEquals(raw, PetState(), decodePet(raw))
        }
    }
}
