package com.pleosplayground.drivequest.core

enum class Alert { FUEL_SHORT, LOW_RANGE, NEARLY_THERE }

data class AlertState(
    val shortArmed: Boolean = true,
    val lowArmed: Boolean = true,
    val nearArmed: Boolean = true,
    val lastSpokeMs: Long? = null,
    val lastDestination: String? = null,
)

data class AlertDecision(val state: AlertState, val alert: Alert?)

const val ALERT_GAP_MS = 180_000L

fun decideAlert(prev: AlertState, trip: TripInfo, nowMs: Long, canSpeak: Boolean): AlertDecision {
    val range = trip.rangeKm
    val dist = trip.distanceKm
    val shortNow = fuelVerdict(range, dist) == FuelVerdict.SHORT
    val lowNow = range != null && range.isFinite() && range >= 0f && range < 50f  // negative range is invalid
    val nearNow = trip.destinationName != null && dist != null && dist.isFinite() && dist >= 0f && dist <= 1f

    val newDest = trip.destinationName != prev.lastDestination
    val s = prev.copy(
        shortArmed = prev.shortArmed || newDest || !shortNow,
        lowArmed = prev.lowArmed || newDest || !lowNow,
        nearArmed = prev.nearArmed || newDest || !nearNow,
        lastDestination = trip.destinationName,
    )

    val last = s.lastSpokeMs
    if (!canSpeak || (last != null && nowMs - last < ALERT_GAP_MS)) return AlertDecision(s, null)

    return when {
        shortNow && s.shortArmed -> AlertDecision(s.copy(shortArmed = false, lowArmed = false, lastSpokeMs = nowMs), Alert.FUEL_SHORT)
        lowNow && s.lowArmed -> AlertDecision(s.copy(lowArmed = false, lastSpokeMs = nowMs), Alert.LOW_RANGE)
        nearNow && s.nearArmed -> AlertDecision(s.copy(nearArmed = false, lastSpokeMs = nowMs), Alert.NEARLY_THERE)
        else -> AlertDecision(s, null)
    }
}

/** Other automatic speech (e.g. the drive-start greeting) counts toward the gap. */
fun AlertState.spoke(nowMs: Long): AlertState = copy(lastSpokeMs = nowMs)

fun alertLine(alert: Alert): String = when (alert) {
    Alert.FUEL_SHORT -> "목적지까지 연료가 모자랄 것 같아요"
    Alert.LOW_RANGE -> "연료가 얼마 안 남았어요"
    Alert.NEARLY_THERE -> "거의 다 왔어요!"
}
