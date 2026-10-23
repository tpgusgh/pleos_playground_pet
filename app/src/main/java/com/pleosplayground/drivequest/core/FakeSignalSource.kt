package com.pleosplayground.drivequest.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.yield

enum class Preset { HEALTHY, LOW_FUEL, NEEDS_CARE, UNSUPPORTED, LONG_TRIP, RUNNING_OUT }
enum class DriveStyle { SMOOTH, HARSH }

interface DevControls {
    fun setUx(state: UxState)
    fun setFuel(percent: Int?)
    fun applyPreset(preset: Preset)
    suspend fun drive(style: DriveStyle)
    fun setTrip(rangeKm: Float?, destinationName: String?, distanceKm: Float?, etaMinutes: Int?)
}

class FakeSignalSource(private val tickDelayMs: Long = 0) : VehicleSignalSource, DevControls {
    private val _ux = MutableStateFlow(UxState.RESTRICTED)
    override val ux: StateFlow<UxState> = _ux

    private val _state = MutableStateFlow(presetSample(Preset.HEALTHY))
    override val state: StateFlow<Sample> = _state

    override val connection: StateFlow<ConnectionState> = MutableStateFlow(ConnectionState.CONNECTED)

    private var clock = 0L

    override fun setUx(state: UxState) {
        _ux.value = state
    }

    override fun setFuel(percent: Int?) {
        _state.value = _state.value.copy(fuelPercent = percent)
    }

    override fun setTrip(rangeKm: Float?, destinationName: String?, distanceKm: Float?, etaMinutes: Int?) {
        _state.value = _state.value.copy(
            rangeKm = rangeKm, destinationName = destinationName,
            distanceToDestinationKm = distanceKm, etaMinutes = etaMinutes,
        )
    }

    /** Replaces the static items only; brake/steer go back to null, ux is kept. */
    override fun applyPreset(preset: Preset) {
        _state.value = presetSample(preset).copy(tMs = _state.value.tMs)
    }

    override suspend fun drive(style: DriveStyle) {
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
    Preset.LONG_TRIP -> Sample(
        fuelPercent = 60, tireOk = true, doorsClosed = true, windowsClosed = true, wiperOk = true,
        rangeKm = 200f, destinationName = "집", distanceToDestinationKm = 80f, etaMinutes = 70,
    )
    Preset.RUNNING_OUT -> Sample(
        fuelPercent = 15, tireOk = true, doorsClosed = true, windowsClosed = true, wiperOk = true,
        rangeKm = 40f, destinationName = "집", distanceToDestinationKm = 120f, etaMinutes = 100,
    )
}
