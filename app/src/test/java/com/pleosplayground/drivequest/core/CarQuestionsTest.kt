package com.pleosplayground.drivequest.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CarQuestionsTest {
    private fun check(expected: CarQuestion?, vararg texts: String) =
        texts.forEach { assertEquals(it, expected, classifyQuestion(it)) }

    @Test fun range_questions() =
        check(CarQuestion.RANGE, "연료 얼마나 가?", "기름 얼마나 남았어", "배터리 얼마나 남았어?", "충전량 얼마야")

    @Test fun destination_questions() =
        check(CarQuestion.DESTINATION, "목적지까지 얼마나 남았어?", "언제 도착해?", "몇 분 걸려?", "몇 킬로 남았어")

    @Test fun enough_questions() {
        check(CarQuestion.ENOUGH, "목적지까지 연료 충분해?", "연료 모자라지 않을까?", "기름 부족해?", "연료로 목적지까지 갈 수 있어?")
        check(CarQuestion.DESTINATION, "배터리로 도착할 수 있어?")
    }

    @Test fun ambiguity_priority() = check(CarQuestion.ENOUGH, "목적지까지 연료로 얼마나 남았어?")

    @Test fun unrelated_and_empty_are_null() = check(null, "", "   ", "안녕", "오늘 날씨 어때?", "노래 틀어줘")

    @Test fun punctuation_and_spacing_are_ignored() = check(CarQuestion.RANGE, "연  료  얼 마 나 가 ?!")

    @Test fun answers_with_values() {
        val t = TripInfo(199.6f, "집", 80.4f, 70)
        assertEquals("지금 연료로 약 200킬로미터 갈 수 있어요", answerFor(CarQuestion.RANGE, t))
        assertEquals("집까지 약 80킬로미터 남았어요, 대략 70분 걸려요", answerFor(CarQuestion.DESTINATION, t))
        assertEquals("집까지 약 80킬로미터 남았어요", answerFor(CarQuestion.DESTINATION, t.copy(etaMinutes = null)))
    }

    @Test fun answers_without_values() {
        val t = TripInfo(null, null, null, null)
        assertEquals("갈 수 있는 거리를 잘 모르겠어요", answerFor(CarQuestion.RANGE, t))
        assertEquals("목적지가 설정 안 돼 있어요", answerFor(CarQuestion.DESTINATION, t))
        assertEquals("목적지가 설정 안 돼 있어요", answerFor(CarQuestion.ENOUGH, t))
    }

    @Test fun enough_answers_follow_the_verdict() {
        fun a(r: Float?, d: Float, m: Int) = answerFor(CarQuestion.ENOUGH, TripInfo(r, "집", d, m))
        assertEquals("네, 목적지까지 연료는 충분해요", a(200f, 80f, 70))
        assertEquals("빠듯해요. 목적지까지 간신히 갈 수 있어요", a(85f, 80f, 70))
        assertEquals("아니요, 목적지까지 연료가 모자랄 것 같아요", a(40f, 120f, 100))
        assertEquals("갈 수 있는 거리를 몰라서 판단하기 어려워요", a(null, 80f, 70))
    }

    @Test fun not_understood_lines_differ_by_face() {
        val l = Face.values().map { notUnderstoodLine(it) }
        assertTrue(l.all { it.isNotBlank() })
        assertEquals(l.size, l.toSet().size)
    }
}
