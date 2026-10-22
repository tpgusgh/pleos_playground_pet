package com.pleosplayground.drivequest.core

enum class UxState { PARKED, RESTRICTED }

fun canPlay(ux: UxState): Boolean = ux == UxState.PARKED

fun summaryLine(score: TripScore, leveledUp: Boolean): String = when {
    score.xp <= 0 -> "이번 주행에서 얻은 경험치가 없어요"
    leveledUp -> "이번 주행으로 레벨업! 경험치 ${score.xp} 획득"
    else -> "경험치 ${score.xp} 획득"
}
