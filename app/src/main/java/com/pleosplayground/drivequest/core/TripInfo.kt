package com.pleosplayground.drivequest.core

data class TripInfo(
    val rangeKm: Float?, val destinationName: String?,
    val distanceKm: Float?, val etaMinutes: Int?,
)

fun tripInfo(s: Sample) = TripInfo(s.rangeKm, s.destinationName, s.distanceToDestinationKm, s.etaMinutes)

enum class FuelVerdict { ENOUGH, TIGHT, SHORT, UNKNOWN }

/** ENOUGH if range >= distance * 1.1 + 5 (km), TIGHT if range >= distance, else SHORT. */
fun fuelVerdict(rangeKm: Float?, distanceKm: Float?): FuelVerdict {
    if (rangeKm == null || distanceKm == null) return FuelVerdict.UNKNOWN
    if (!rangeKm.isFinite() || !distanceKm.isFinite() || rangeKm < 0f || distanceKm < 0f) return FuelVerdict.UNKNOWN
    return when {
        rangeKm >= distanceKm * 11f / 10f + 5f -> FuelVerdict.ENOUGH  // x*11/10 keeps 100 -> 110 exact in Float
        rangeKm >= distanceKm -> FuelVerdict.TIGHT
        else -> FuelVerdict.SHORT
    }
}

/** The value only if finite and >= 0, else null (treated as unknown). */
internal fun Float?.validKm(): Float? = this?.takeIf { it.isFinite() && it >= 0f }
