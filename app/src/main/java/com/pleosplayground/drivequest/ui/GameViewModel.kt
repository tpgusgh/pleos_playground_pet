package com.pleosplayground.drivequest.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pleosplayground.drivequest.core.BattleResult
import com.pleosplayground.drivequest.core.FakeSignalSource
import com.pleosplayground.drivequest.core.Hero
import com.pleosplayground.drivequest.core.Monster
import com.pleosplayground.drivequest.core.Scenario
import com.pleosplayground.drivequest.core.TripScore
import com.pleosplayground.drivequest.core.UxState
import com.pleosplayground.drivequest.core.bossChallenger
import com.pleosplayground.drivequest.core.canPlay
import com.pleosplayground.drivequest.core.fight
import com.pleosplayground.drivequest.core.gain
import com.pleosplayground.drivequest.core.score
import com.pleosplayground.drivequest.core.summaryLine
import com.pleosplayground.drivequest.core.weeklyBoss
import com.pleosplayground.drivequest.data.HeroStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch

// ponytail: typed to FakeSignalSource (fake mode only); widen to VehicleSignalSource when a real source lands.
class GameViewModel(
    private val source: FakeSignalSource,
    private val store: HeroStore,
    private val speaker: Speaker,
) : ViewModel() {
    private val _hero = MutableStateFlow(Hero())
    val hero: StateFlow<Hero> = _hero.asStateFlow()
    private val _lastScore = MutableStateFlow<TripScore?>(null)
    val lastScore: StateFlow<TripScore?> = _lastScore.asStateFlow()
    private val _lastBattle = MutableStateFlow<BattleResult?>(null)
    val lastBattle: StateFlow<BattleResult?> = _lastBattle.asStateFlow()
    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()
    private val _scenario = MutableStateFlow(source.scenario)
    val scenario: StateFlow<Scenario> = _scenario.asStateFlow()
    val ux: StateFlow<UxState> = source.ux

    // ponytail: session-only; not persisted, resets on app restart.
    private var weekXp = 0
    private var battleSeed = 1L

    init {
        viewModelScope.launch { _hero.value = store.load() }
    }

    fun setScenario(s: Scenario) {
        source.scenario = s
        _scenario.value = s
    }

    fun setParked(parked: Boolean) = source.setUx(if (parked) UxState.PARKED else UxState.RESTRICTED)

    fun runTrip() {
        if (!_running.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            try {
                val trip = score(source.trip().toList())
                val before = _hero.value
                val after = before.gain(trip)
                _hero.value = after
                _lastScore.value = trip
                weekXp += trip.xp
                store.save(after)
                speaker.speak(summaryLine(trip, after.level > before.level))
            } finally {
                _running.value = false
            }
        }
    }

    fun battle() {
        if (!canPlay(ux.value)) return
        val l = _hero.value.level - 1
        val monster = Monster("슬라임", 30 + 20 * l, 5 + 2 * l, l, 1 + l)
        _lastBattle.value = fight(_hero.value, monster, battleSeed++)
    }

    fun bossBattle() {
        if (!canPlay(ux.value)) return
        _lastBattle.value = fight(bossChallenger(_hero.value, weekXp), weeklyBoss(0), battleSeed++)
    }

    override fun onCleared() = speaker.shutdown()
}
