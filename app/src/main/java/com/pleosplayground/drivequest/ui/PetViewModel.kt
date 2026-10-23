package com.pleosplayground.drivequest.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pleosplayground.drivequest.core.ALERT_GAP_MS
import com.pleosplayground.drivequest.core.AlertState
import com.pleosplayground.drivequest.core.DriveStyle
import com.pleosplayground.drivequest.core.Face
import com.pleosplayground.drivequest.core.FakeSignalSource
import com.pleosplayground.drivequest.core.LineKind
import com.pleosplayground.drivequest.core.PetState
import com.pleosplayground.drivequest.core.Preset
import com.pleosplayground.drivequest.core.Species
import com.pleosplayground.drivequest.core.TripInfo
import com.pleosplayground.drivequest.core.UxState
import com.pleosplayground.drivequest.core.alertLine
import com.pleosplayground.drivequest.core.answerFor
import com.pleosplayground.drivequest.core.applyDriving
import com.pleosplayground.drivequest.core.canPlay
import com.pleosplayground.drivequest.core.classifyQuestion
import com.pleosplayground.drivequest.core.decideAlert
import com.pleosplayground.drivequest.core.faceFor
import com.pleosplayground.drivequest.core.food
import com.pleosplayground.drivequest.core.gapBlocks
import com.pleosplayground.drivequest.core.health
import com.pleosplayground.drivequest.core.lineFor
import com.pleosplayground.drivequest.core.moodDelta
import com.pleosplayground.drivequest.core.pet
import com.pleosplayground.drivequest.core.settle
import com.pleosplayground.drivequest.core.speciesFromId
import com.pleosplayground.drivequest.core.spoke
import com.pleosplayground.drivequest.core.talk
import com.pleosplayground.drivequest.core.tripInfo
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

    // Rabbit until chosen. needsPicker stays false until the stored species has been read.
    private val _species = MutableStateFlow(Species.RABBIT)
    val species: StateFlow<Species> = _species.asStateFlow()
    private val _speciesChosen = MutableStateFlow(false)
    val speciesChosen: StateFlow<Boolean> = _speciesChosen.asStateFlow()
    private val _speciesLoaded = MutableStateFlow(false)
    val needsPicker: StateFlow<Boolean> = combine(_speciesLoaded, _speciesChosen, source.ux) { loaded, chosen, u ->
        loaded && !chosen && u == UxState.PARKED
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _trip = MutableStateFlow(tripInfo(source.state.value))
    val trip: StateFlow<TripInfo> = _trip.asStateFlow()
    private val _proactive = MutableStateFlow(true)
    val proactiveEnabled: StateFlow<Boolean> = _proactive.asStateFlow()
    private var alertState = AlertState()

    private var lastGreetMs: Long? = null

    // A failed load keeps the default pet. Actions join this so a slow load cannot overwrite saved progress.
    private val loaded = viewModelScope.launch {
        val stored = try {
            store.loadSpecies()
        } catch (e: IOException) {
            null
        }
        if (stored != null) {
            _species.value = stored
            _speciesChosen.value = true
        }
        _speciesLoaded.value = true
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
                _trip.value = tripInfo(s)
                checkAlert()
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

    // ponytail: no "stay quiet while the user is talking" rule yet; the LLM plan adds it.
    // A blocked alert stays armed and is retried only on the next source.state emission, on setProactive(true)
    // or on a drive start (with the fake source: after the next dev-tool action).
    private fun checkAlert() {
        val d = decideAlert(alertState, _trip.value, clock(), _proactive.value)
        alertState = d.state
        d.alert?.let { say(alertLine(it)) }
    }

    private fun greetDriveStart() {
        if (!_proactive.value) return // the proactive switch also silences the greeting
        checkAlert() // a waiting alert outranks the greeting; if it speaks, the shared gap skips the greeting
        val now = clock()
        val last = lastGreetMs
        if (!gapBlocks(now, last, GREET_GAP_MS) && !gapBlocks(now, alertState.lastSpokeMs, ALERT_GAP_MS)) {
            lastGreetMs = now
            alertState = alertState.spoke(now)
            say(lineFor(_species.value, LineKind.DRIVE_START, currentFace()))
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
        val face = currentFace()
        val r = _pet.value.talk(clock(), face)
        if (r.reply == null) return@act // PetState.talk is only the cooldown/mood gate; the line comes from the species
        if (r.state != _pet.value) {
            _pet.value = r.state
            persist()
        }
        say(lineFor(_species.value, LineKind.GREETING, face))
    }

    fun ask(text: String) = act {
        val r = _pet.value.talk(clock(), currentFace())
        if (r.reply == null) return@act // same cooldown/mood gate as talk(); the canned reply is discarded
        if (r.state != _pet.value) {
            _pet.value = r.state
            persist()
        }
        val q = classifyQuestion(text)
        say(if (q != null) answerFor(q, _trip.value) else lineFor(_species.value, LineKind.NOT_UNDERSTOOD, currentFace()))
    }

    fun chooseSpecies(id: String) = act {
        if (!_speciesChosen.value) switchSpecies(id)
    }

    fun changeSpecies(id: String) = act {
        if (speciesFromId(id) != _species.value) switchSpecies(id)
    }

    // The species state is set before the first suspend call, so a second quick call sees the new state.
    private suspend fun switchSpecies(id: String) {
        if (!canPlay(source.ux.value)) return
        val next = speciesFromId(id) ?: return
        _species.value = next
        _speciesChosen.value = true
        _pet.value = PetState(settledAtMs = clock())
        _line.value = null
        try {
            store.saveSpecies(next)
        } catch (e: IOException) {
            // ponytail: 종 저장 실패는 무시, 다음 실행에 고르기 화면이 다시 뜰 수 있다
        }
        persist()
    }

    fun setProactive(enabled: Boolean) = act {
        _proactive.value = enabled
        if (enabled) checkAlert()
    }

    fun setTrip(rangeKm: Float?, destinationName: String?, distanceKm: Float?, etaMinutes: Int?) =
        act { source.setTrip(rangeKm, destinationName, distanceKm, etaMinutes) }

    fun setParked(parked: Boolean) = act { source.setUx(if (parked) UxState.PARKED else UxState.RESTRICTED) }

    fun setFuel(percent: Int?) = act { source.setFuel(percent) }

    fun applyPreset(preset: Preset) = act { source.applyPreset(preset) }

    fun startDrive(style: DriveStyle) = act { source.drive(style) }

    override fun onCleared() = narrator.shutdown()
}
