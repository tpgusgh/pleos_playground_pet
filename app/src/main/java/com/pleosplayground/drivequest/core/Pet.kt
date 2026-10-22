package com.pleosplayground.drivequest.core

enum class Face { HAPPY, SULKY, HUNGRY }

private const val HARD_BRAKE = 4.0f
private const val SHARP_STEER = 120f

fun food(s: Sample): Int? = s.fuelPercent?.coerceIn(0, 100)

fun health(s: Sample): Int? {
    val known = listOfNotNull(s.tireOk, s.doorsClosed, s.windowsClosed, s.wiperOk)
    if (known.isEmpty()) return null
    return known.count { it } * 100 / known.size
}

fun moodDelta(s: Sample): Int {
    var d = 0
    s.brakeDecel?.takeIf { it.isFinite() && it > 0f }?.let { d += if (it > HARD_BRAKE) -5 else 1 }
    s.steeringRate?.takeIf { it.isFinite() && it > 0f }?.let { d += if (it > SHARP_STEER) -3 else 1 }
    return d
}

fun faceFor(food: Int?, health: Int?, mood: Int): Face = when {
    food != null && food < 20 -> Face.HUNGRY
    (health != null && health < 50) || mood < 40 -> Face.SULKY
    else -> Face.HAPPY
}
