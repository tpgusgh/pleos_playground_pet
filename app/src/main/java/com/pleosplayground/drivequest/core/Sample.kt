package com.pleosplayground.drivequest.core

/** One tick of vehicle signals. A null field means the signal is unsupported. */
data class Sample(
    val tMs: Long = 0,
    val brakeDecel: Float? = null,    // m/s², 제동 감속도
    val steeringRate: Float? = null,  // deg/s, 절댓값
    val fuelPercent: Int? = null,     // 연료 또는 EV 배터리 잔량, 0~100
    val tireOk: Boolean? = null,
    val doorsClosed: Boolean? = null,
    val windowsClosed: Boolean? = null,
    val wiperOk: Boolean? = null,
)
