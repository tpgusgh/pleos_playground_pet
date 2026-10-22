package com.pleosplayground.drivequest.core

/** One tick of vehicle signals. A null field means the signal is unsupported. */
data class Sample(
    val tMs: Long,
    val brakeDecel: Float? = null,    // m/s², 제동 감속도
    val steeringRate: Float? = null,  // deg/s, 절댓값
    val turning: Boolean? = null,
    val turnSignalOn: Boolean? = null,
    val odometerKm: Float? = null,
    val ecoMode: Boolean? = null,
    val beltOn: Boolean? = null,
    val headlightOn: Boolean? = null,
    val wiperOn: Boolean? = null,
    val tireOk: Boolean? = null,
)
