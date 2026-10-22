package com.pleosplayground.drivequest.ui

import com.pleosplayground.drivequest.core.FakeSignalSource
import com.pleosplayground.drivequest.core.Hero
import com.pleosplayground.drivequest.core.Scenario
import com.pleosplayground.drivequest.data.HeroRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.io.IOException

private class FakeRepo(
    private val initial: Hero = Hero(),
    private val failSave: Boolean = false,
    private val failLoad: Boolean = false,
    private val gate: CompletableDeferred<Unit>? = null,
) : HeroRepository {
    val saved = mutableListOf<Hero>()

    override suspend fun load(): Hero {
        gate?.await()
        if (failLoad) throw IOException("load")
        return initial
    }

    override suspend fun save(hero: Hero) {
        saved += hero
        if (failSave) throw IOException("save")
    }
}

private class FakeNarrator : Narrator {
    val lines = mutableListOf<String>()
    override fun speak(text: String) { lines += text }
    override fun shutdown() {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm(repo: FakeRepo, narrator: FakeNarrator = FakeNarrator()) =
        GameViewModel(FakeSignalSource(Scenario.SMOOTH), repo, narrator)

    @Test
    fun save_failure_keeps_hero_and_still_speaks() = runTest(dispatcher) {
        val narrator = FakeNarrator()
        val vm = vm(FakeRepo(failSave = true), narrator)
        vm.runTrip()
        advanceUntilIdle()
        assertEquals(2, vm.hero.value.level) // SMOOTH trip = 154 xp
        assertEquals(1, narrator.lines.size)
        assertFalse(vm.running.value)
    }

    @Test
    fun trip_waits_for_initial_load() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repo = FakeRepo(initial = Hero(5, 0, 140, 18, 9, 9), gate = gate)
        val vm = vm(repo)
        vm.runTrip()
        advanceUntilIdle()
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, repo.saved.size)
        assertEquals(5, repo.saved.single().level)
        assertEquals(154, repo.saved.single().xp)
        assertEquals(5, vm.hero.value.level)
    }

    @Test
    fun load_failure_falls_back_to_default_hero() = runTest(dispatcher) {
        val repo = FakeRepo(failLoad = true)
        val vm = vm(repo)
        vm.runTrip()
        advanceUntilIdle()
        assertEquals(2, vm.hero.value.level)
        assertEquals(1, repo.saved.size)
    }
}
