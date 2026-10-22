package com.pleosplayground.drivequest.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.yield

enum class Preset { HEALTHY, LOW_FUEL, NEEDS_CARE, UNSUPPORTED }
enum class DriveStyle { SMOOTH, HARSH }

class FakeSignalSource(private val tickDelayMs: Long = 0) : VehicleSignalSource {
    private val _ux = MutableStateFlow(UxState.RESTRICTED)
    override val ux: StateFlow<UxState> = _ux

    private val _state = MutableStateFlow(presetSample(Preset.HEALTHY))
    override val state: StateFlow<Sample> = _state

    private var clock = 0L

    fun setUx(state: UxState) {
        _ux.value = state
    }

    fun setFuel(percent: Int?) {
        _state.value = _state.value.copy(fuelPercent = percent)
    }

    /** Replaces the static items only; brake/steer go back to null, ux is kept. */
    fun applyPreset(preset: Preset) {
        _state.value = presetSample(preset).copy(tMs = _state.value.tMs)
    }

    suspend fun drive(style: DriveStyle) {
        for (i in 0 until TICKS) {
            val (brake, steer) = when (style) {
                DriveStyle.SMOOTH -> (if (i % 4 == 3) 2.0f else null) to 20f
                DriveStyle.HARSH ->
                    (if (i % 5 == 4) 6.0f else null) to (if (i % 3 == 0) 150f else 30f)
            }
            _state.value = _state.value.copy(tMs = ++clock, brakeDecel = brake, steeringRate = steer)
            if (tickDelayMs > 0) delay(tickDelayMs) else yield()
        }
        _state.value = _state.value.copy(tMs = ++clock, brakeDecel = null, steeringRate = null)
    }
}

private const val TICKS = 20

private fun presetSample(p: Preset): Sample = when (p) {
    Preset.HEALTHY -> Sample(fuelPercent = 80, tireOk = true, doorsClosed = true, windowsClosed = true, wiperOk = true)
    Preset.LOW_FUEL -> Sample(fuelPercent = 10, tireOk = true, doorsClosed = true, windowsClosed = true, wiperOk = true)
    Preset.NEEDS_CARE -> Sample(fuelPercent = 55, tireOk = false, doorsClosed = false, windowsClosed = false, wiperOk = true)
    Preset.UNSUPPORTED -> Sample()
}
