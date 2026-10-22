package com.pleosplayground.drivequest.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pleosplayground.drivequest.core.DriveStyle
import com.pleosplayground.drivequest.core.Face
import com.pleosplayground.drivequest.core.FakeSignalSource
import com.pleosplayground.drivequest.core.PetState
import com.pleosplayground.drivequest.core.Preset
import com.pleosplayground.drivequest.core.UxState
import com.pleosplayground.drivequest.core.applyDriving
import com.pleosplayground.drivequest.core.canPlay
import com.pleosplayground.drivequest.core.driveStartLine
import com.pleosplayground.drivequest.core.faceFor
import com.pleosplayground.drivequest.core.food
import com.pleosplayground.drivequest.core.health
import com.pleosplayground.drivequest.core.moodDelta
import com.pleosplayground.drivequest.core.pet
import com.pleosplayground.drivequest.core.settle
import com.pleosplayground.drivequest.core.talk
import com.pleosplayground.drivequest.data.PetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

private const val GREET_GAP_MS = 600_000L

// ponytail: typed to FakeSignalSource (fake mode only); widen to VehicleSignalSource when a real source lands.
class PetViewModel(
    private val source: FakeSignalSource,
    private val store: PetRepository,
    private val narrator: Narrator,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val _pet = MutableStateFlow(PetState())
    val pet: StateFlow<PetState> = _pet.asStateFlow()
    private val _food = MutableStateFlow(food(source.state.value))
    val food: StateFlow<Int?> = _food.asStateFlow()
    private val _health = MutableStateFlow(health(source.state.value))
    val health: StateFlow<Int?> = _health.asStateFlow()
    val face: StateFlow<Face> = combine(_food, _health, _pet) { f, h, p -> faceFor(f, h, p.mood) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, faceFor(_food.value, _health.value, _pet.value.mood))
    val ux: StateFlow<UxState> = source.ux
    private val _line = MutableStateFlow<String?>(null)
    val line: StateFlow<String?> = _line.asStateFlow()

    private var lastGreetMs: Long? = null

    // A failed load keeps the default pet. Actions join this so a slow load cannot overwrite saved progress.
    private val loaded = viewModelScope.launch {
        _pet.value = try {
            store.load()
        } catch (e: IOException) {
            PetState()
        }.settle(clock())
        persist()
        // Subscribed only after load, so the initial RESTRICTED is not a drive start.
        viewModelScope.launch {
            source.state.collect { s ->
                _food.value = food(s)
                _health.value = health(s)
                val d = moodDelta(s)
                if (d != 0) {
                    _pet.value = _pet.value.applyDriving(d, clock())
                    persist()
                }
            }
        }
        viewModelScope.launch {
            var prev = source.ux.value
            source.ux.collect { u ->
                if (prev == UxState.PARKED && u == UxState.RESTRICTED) greetDriveStart()
                prev = u
            }
        }
    }

    // ponytail: a failed save is swallowed; the next change re-saves the in-memory pet.
    private suspend fun persist() {
        try {
            store.save(_pet.value)
        } catch (e: IOException) {
            // keep going
        }
    }

    private fun say(text: String) {
        narrator.speak(text)
        _line.value = text
    }

    private fun currentFace() = faceFor(_food.value, _health.value, _pet.value.mood)

    private fun greetDriveStart() {
        val now = clock()
        val last = lastGreetMs
        if (last == null || now - last >= GREET_GAP_MS) {
            lastGreetMs = now
            say(driveStartLine(currentFace()))
        }
    }

    private fun act(block: suspend () -> Unit) {
        viewModelScope.launch {
            loaded.join()
            block()
        }
    }

    fun petIt() = act {
        if (!canPlay(source.ux.value)) return@act
        val next = _pet.value.pet(clock())
        if (next != _pet.value) {
            _pet.value = next
            persist()
        }
    }

    fun talk() = act {
        val r = _pet.value.talk(clock(), currentFace())
        if (r.state != _pet.value) {
            _pet.value = r.state
            persist()
        }
        r.reply?.let(::say)
    }

    fun setParked(parked: Boolean) = act { source.setUx(if (parked) UxState.PARKED else UxState.RESTRICTED) }

    fun setFuel(percent: Int?) = act { source.setFuel(percent) }

    fun applyPreset(preset: Preset) = act { source.applyPreset(preset) }

    fun startDrive(style: DriveStyle) = act { source.drive(style) }

    override fun onCleared() = narrator.shutdown()
}
