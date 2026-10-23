package com.pleosplayground.drivequest.core

import kotlin.math.roundToInt

enum class CarQuestion { RANGE, DESTINATION, ENOUGH }

private val FUEL_WORDS = listOf("연료", "기름", "배터리", "충전")
private val NOUNS = FUEL_WORDS + listOf("목적지", "도착")
private val ENOUGH_WORDS = listOf("충분", "모자라", "부족", "버틸")
private val STRIP = Regex("[\\s?!.,]")

/** Keyword rules on text with whitespace and `? ! . ,` removed, lowercased. First match wins: ENOUGH, DESTINATION, RANGE. */
fun classifyQuestion(text: String): CarQuestion? {
    val t = text.replace(STRIP, "").lowercase()
    fun has(words: List<String>) = words.any { it in t }
    return when {
        (has(ENOUGH_WORDS) && has(NOUNS)) ||
            (has(listOf("연료", "기름", "배터리", "충전")) && "목적지" in t) -> CarQuestion.ENOUGH
        has(listOf("목적지", "도착", "몇분", "몇킬로")) -> CarQuestion.DESTINATION
        has(FUEL_WORDS) -> CarQuestion.RANGE
        has(listOf("얼마나가", "얼마나갈", "얼마나더가", "얼마나더갈")) -> CarQuestion.RANGE
        else -> null
    }
}

fun answerFor(q: CarQuestion, trip: TripInfo): String {
    if (q == CarQuestion.RANGE) {
        return trip.rangeKm.validKm()?.let { "지금 연료로 약 ${it.roundToInt()}킬로미터 갈 수 있어요" } ?: "갈 수 있는 거리를 잘 모르겠어요"
    }
    val name = trip.destinationName
    val dist = trip.distanceKm.validKm()
    if (name == null || dist == null) return "목적지가 설정 안 돼 있어요"
    if (q == CarQuestion.DESTINATION) {
        val eta = trip.etaMinutes?.takeIf { it >= 0 }?.let { ", 대략 ${it}분 걸려요" } ?: ""
        return "${name}까지 약 ${dist.roundToInt()}킬로미터 남았어요$eta"
    }
    return when (fuelVerdict(trip.rangeKm, dist)) {
        FuelVerdict.UNKNOWN -> "갈 수 있는 거리를 몰라서 판단하기 어려워요"
        FuelVerdict.ENOUGH -> "네, 목적지까지 연료는 충분해요"
        FuelVerdict.TIGHT -> "빠듯해요. 목적지까지 간신히 갈 수 있어요"
        FuelVerdict.SHORT -> "아니요, 목적지까지 연료가 모자랄 것 같아요"
    }
}

fun notUnderstoodLine(face: Face): String = when (face) {
    Face.HAPPY -> "음… 잘 모르겠어요"
    Face.SULKY -> "...잘 모르겠어요"
    Face.HUNGRY -> "배고파서 잘 모르겠어요..."
}
