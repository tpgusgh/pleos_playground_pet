package com.pleosplayground.drivequest.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

enum class Scenario { SMOOTH, AGGRESSIVE, RAINY_NIGHT, UNSUPPORTED }

class FakeSignalSource(
    private val scenario: Scenario,
    private val tickDelayMs: Long = 0,
) : VehicleSignalSource {
    private val _ux = MutableStateFlow(UxState.RESTRICTED)
    override val ux: StateFlow<UxState> = _ux

    fun setUx(state: UxState) {
        _ux.value = state
    }

    override fun trip(): Flow<Sample> = flow {
        for (sample in script(scenario)) {
            emit(sample)
            if (tickDelayMs > 0) delay(tickDelayMs)
        }
    }
}

private const val TICKS = 20

private fun script(scenario: Scenario): List<Sample> = List(TICKS) { i ->
    val t = i * 1000L
    val km = i * 0.5f
    val turning = i % 5 == 2
    when (scenario) {
        Scenario.SMOOTH -> Sample(
            tMs = t, brakeDecel = if (i % 4 == 3) 2.0f else null, steeringRate = 20f,
            turning = turning, turnSignalOn = turning, odometerKm = km,
            ecoMode = true, beltOn = true, tireOk = true,
        )
        Scenario.AGGRESSIVE -> Sample(
            tMs = t, brakeDecel = if (i % 5 == 4) 6.0f else null,
            steeringRate = if (i % 3 == 0) 150f else 30f,
            turning = turning, turnSignalOn = false, odometerKm = km,
            ecoMode = false, beltOn = false,
        )
        Scenario.RAINY_NIGHT -> Sample(
            tMs = t, brakeDecel = if (i % 4 == 3) 2.5f else null, steeringRate = 25f,
            turning = turning, turnSignalOn = turning, odometerKm = km,
            ecoMode = false, beltOn = true, headlightOn = true, wiperOn = true,
        )
        Scenario.UNSUPPORTED -> Sample(tMs = t)
    }
}
