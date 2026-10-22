package com.pleosplayground.drivequest.core

private const val DECAY_STEP_MS = 1_800_000L
private const val COOLDOWN_MS = 5_000L

data class PetState(
    val mood: Int = 70,
    val settledAtMs: Long = 0,
    val petReadyAtMs: Long = 0,
    val talkReadyAtMs: Long = 0,
)

data class TalkResult(val state: PetState, val reply: String?)

fun PetState.settle(nowMs: Long): PetState {
    if (settledAtMs <= 0) return copy(settledAtMs = nowMs)
    if (nowMs <= settledAtMs) return this // 시계가 되감겨도 나중 기준점 유지
    val points = (nowMs - settledAtMs) / DECAY_STEP_MS
    return copy(
        mood = (mood - points).coerceAtLeast(0).toInt(),
        settledAtMs = settledAtMs + points * DECAY_STEP_MS,
    )
}

// 쿨다운 1회분보다 먼 미래의 readyAt은 시계 오류로 보고 무시
private fun isCoolingDown(readyAtMs: Long, nowMs: Long) =
    readyAtMs > nowMs && readyAtMs - nowMs <= COOLDOWN_MS

fun PetState.applyDriving(delta: Int, nowMs: Long): PetState =
    settle(nowMs).let { it.copy(mood = (it.mood + delta).coerceIn(0, 100)) }

fun PetState.pet(nowMs: Long): PetState {
    if (isCoolingDown(petReadyAtMs, nowMs)) return this
    val s = settle(nowMs)
    return s.copy(mood = (s.mood + 10).coerceIn(0, 100), petReadyAtMs = nowMs + COOLDOWN_MS)
}

fun PetState.talk(nowMs: Long, face: Face): TalkResult {
    if (isCoolingDown(talkReadyAtMs, nowMs)) return TalkResult(this, null)
    val s = settle(nowMs)
    return TalkResult(
        s.copy(mood = (s.mood + 5).coerceIn(0, 100), talkReadyAtMs = nowMs + COOLDOWN_MS),
        greeting(face),
    )
}

fun greeting(face: Face): String = when (face) {
    Face.HAPPY -> "안녕! 오늘도 같이 달려요"
    Face.SULKY -> "...안녕. 조금 시무룩해요"
    Face.HUNGRY -> "안녕... 배고파요"
}

fun driveStartLine(face: Face): String = when (face) {
    Face.HAPPY -> "출발이야? 좋아요!"
    Face.SULKY -> "...출발해요?"
    Face.HUNGRY -> "배고픈데... 출발해요?"
}

fun PetState.encode(): String = "$mood,$settledAtMs,$petReadyAtMs,$talkReadyAtMs"

fun decodePet(raw: String?): PetState {
    val p = raw?.split(",") ?: return PetState()
    if (p.size != 4) return PetState()
    val mood = p[0].toIntOrNull()
    val nums = p.drop(1).map { it.toLongOrNull() }
    if (mood == null || mood !in 0..100 || nums.any { it == null || it < 0 }) return PetState()
    return PetState(mood, nums[0]!!, nums[1]!!, nums[2]!!)
}
