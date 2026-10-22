package com.pleosplayground.drivequest.core

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class Flag { STORM, NIGHT, ECO }

data class TripScore(
    val xp: Int, val gold: Int, val km: Float,
    val hardBrakes: Int, val smoothBrakes: Int, val sharpSteers: Int,
    val bestCombo: Int, val flags: Set<Flag>,
)

private const val HARD_BRAKE_DECEL = 4.0f
private const val SHARP_STEER_RATE = 120f
private const val DISTANCE_XP_PER_KM = 10
private const val DISTANCE_XP_CAP = 200

fun score(samples: List<Sample>): TripScore {
    var km = 0f
    var lastOdo: Float? = null
    var xp = 0
    var hard = 0
    var smooth = 0
    var sharp = 0
    var combo = 0
    var best = 0
    var beltSeen = false
    var prevTurning = false
    var eco = false
    var storm = false
    var night = false
    var tire = false

    for (s in samples) {
        s.odometerKm?.takeIf { it.isFinite() }?.let { v ->
            lastOdo?.let { prev -> if (v > prev) km += v - prev }
            lastOdo = v
        }
        if (!beltSeen && s.beltOn != null) {
            beltSeen = true
            if (s.beltOn) xp += 20
        }
        s.brakeDecel?.let { d ->
            if (!d.isNaN() && d > 0f) {
                if (d > HARD_BRAKE_DECEL) { hard++; xp -= 5 } else { smooth++; xp += 1 }
            }
        }
        if ((s.steeringRate ?: 0f) > SHARP_STEER_RATE) { sharp++; xp -= 3 }
        val turning = s.turning == true
        if (turning && !prevTurning) {
            if (s.turnSignalOn == true) {
                combo++
                best = max(best, combo)
                xp += 2
            } else {
                combo = 0
            }
        }
        prevTurning = turning
        if (s.ecoMode == true) eco = true
        if (s.wiperOn == true) storm = true
        if (s.headlightOn == true) night = true
        if (s.tireOk == true) tire = true
    }

    xp += min((km * DISTANCE_XP_PER_KM).toInt(), DISTANCE_XP_CAP)
    xp = max(0, xp)
    if (eco) xp = (xp * 1.2f).roundToInt()
    val flags = buildSet {
        if (storm) add(Flag.STORM)
        if (night) add(Flag.NIGHT)
        if (eco) add(Flag.ECO)
    }
    return TripScore(xp, xp / 2 + if (tire) 5 else 0, km, hard, smooth, sharp, best, flags)
}
