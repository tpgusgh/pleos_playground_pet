package com.pleosplayground.drivequest.ui

import com.pleosplayground.drivequest.core.Alert
import com.pleosplayground.drivequest.core.FakeSignalSource
import com.pleosplayground.drivequest.core.PetState
import com.pleosplayground.drivequest.core.Preset
import com.pleosplayground.drivequest.core.Species
import com.pleosplayground.drivequest.core.UxState
import com.pleosplayground.drivequest.core.alertLine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PetViewModelSpeciesTest {
    private val dispatcher = StandardTestDispatcher()
    private var now = 1_000L
    private val narrator = FakeNarrator()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.newVm(repo: FakeRepo = FakeRepo(), source: FakeSignalSource = FakeSignalSource()): PetViewModel {
        val vm = PetViewModel(source, repo, narrator) { now }
        advanceUntilIdle()
        return vm
    }

    private fun TestScope.park(vm: PetViewModel, parked: Boolean = true) { vm.setParked(parked); advanceUntilIdle() }
    private fun TestScope.choose(vm: PetViewModel, id: String) { vm.chooseSpecies(id); advanceUntilIdle() }
    private fun TestScope.change(vm: PetViewModel, id: String) { vm.changeSpecies(id); advanceUntilIdle() }
    private fun TestScope.talk(vm: PetViewModel) { vm.talk(); advanceUntilIdle() }

    @Test
    fun not_chosen_needs_picker_only_when_parked() = runTest(dispatcher) {
        val vm = newVm()
        assertFalse(vm.needsPicker.value)
        assertEquals(Species.RABBIT, vm.species.value)
        assertFalse(vm.speciesChosen.value)
        park(vm)
        assertTrue(vm.needsPicker.value)
        park(vm, false)
        assertFalse(vm.needsPicker.value)
    }

    @Test
    fun not_chosen_while_driving_talks_as_the_rabbit() = runTest(dispatcher) {
        val vm = newVm()
        talk(vm)
        assertEquals(listOf("안녕! 오늘도 같이 달려요"), narrator.lines)
    }

    @Test
    fun stored_species_is_restored() = runTest(dispatcher) {
        val vm = newVm(FakeRepo(initialSpecies = Species.BEAR))
        park(vm)
        assertEquals(Species.BEAR, vm.species.value)
        assertTrue(vm.speciesChosen.value)
        assertFalse(vm.needsPicker.value)
    }

    @Test
    fun picker_waits_for_the_stored_species() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val source = FakeSignalSource()
        val vm = newVm(FakeRepo(initialSpecies = Species.CAT, gate = gate), source)
        source.setUx(UxState.PARKED)
        advanceUntilIdle()
        assertFalse(vm.needsPicker.value)
        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.needsPicker.value)
        assertEquals(Species.CAT, vm.species.value)
    }

    @Test
    fun choose_saves_switches_and_resets_the_pet() = runTest(dispatcher) {
        val repo = FakeRepo(initial = PetState(30, 1_000, 0, 0))
        val vm = newVm(repo)
        park(vm)
        now = 5_000
        choose(vm, "dog")
        assertEquals(listOf(Species.DOG), repo.savedSpecies)
        assertEquals(Species.DOG, vm.species.value)
        assertTrue(vm.speciesChosen.value)
        assertFalse(vm.needsPicker.value)
        assertEquals(PetState(70, 5_000, 0, 0), vm.pet.value)
        assertEquals(PetState(70, 5_000, 0, 0), repo.saved.last())
    }

    @Test
    fun choose_is_ignored_while_driving() = runTest(dispatcher) {
        val repo = FakeRepo()
        val vm = newVm(repo)
        choose(vm, "cat")
        assertEquals(Species.RABBIT, vm.species.value)
        assertFalse(vm.speciesChosen.value)
        assertTrue(repo.savedSpecies.isEmpty())
    }

    @Test
    fun double_tap_on_confirm_keeps_the_first_choice() = runTest(dispatcher) {
        val repo = FakeRepo()
        val vm = newVm(repo)
        park(vm)
        vm.chooseSpecies("cat")
        vm.chooseSpecies("dog")
        advanceUntilIdle()
        assertEquals(Species.CAT, vm.species.value)
        assertEquals(listOf(Species.CAT), repo.savedSpecies)
    }

    @Test
    fun double_tap_with_a_suspending_save_keeps_the_first_choice() = runTest(dispatcher) {
        val saveGate = CompletableDeferred<Unit>()
        val repo = FakeRepo(saveGate = saveGate)
        val vm = newVm(repo)
        park(vm)
        vm.chooseSpecies("cat")
        runCurrent() // the first call is now suspended inside saveSpecies
        vm.chooseSpecies("dog")
        runCurrent()
        assertEquals(Species.CAT, vm.species.value)
        assertEquals(listOf(Species.CAT), repo.savedSpecies)
        assertFalse(vm.needsPicker.value)
        saveGate.complete(Unit)
        advanceUntilIdle()
        assertEquals(Species.CAT, vm.species.value)
        assertEquals(listOf(Species.CAT), repo.savedSpecies)
        assertEquals(70, repo.saved.last().mood)
    }

    @Test
    fun unknown_id_is_ignored() = runTest(dispatcher) {
        val repo = FakeRepo()
        val vm = newVm(repo)
        park(vm)
        choose(vm, "dragon")
        assertFalse(vm.speciesChosen.value)
        assertTrue(repo.savedSpecies.isEmpty())
        assertTrue(vm.needsPicker.value)
    }

    @Test
    fun species_load_failure_falls_back_to_the_rabbit() = runTest(dispatcher) {
        val vm = newVm(FakeRepo(failLoad = true))
        assertEquals(Species.RABBIT, vm.species.value)
        assertFalse(vm.speciesChosen.value)
        assertEquals(70, vm.pet.value.mood)
        park(vm)
        assertTrue(vm.needsPicker.value)
    }

    @Test
    fun species_save_failure_keeps_the_choice() = runTest(dispatcher) {
        val vm = newVm(FakeRepo(failSave = true))
        park(vm)
        choose(vm, "fox")
        assertEquals(Species.FOX, vm.species.value)
        assertTrue(vm.speciesChosen.value)
        assertEquals(70, vm.pet.value.mood)
    }

    @Test
    fun change_resets_mood_and_switches_lines_at_once() = runTest(dispatcher) {
        val repo = FakeRepo(initial = PetState(40, 1_000, 0, 0), initialSpecies = Species.RABBIT)
        val vm = newVm(repo)
        park(vm)
        vm.petIt(); advanceUntilIdle()
        assertEquals(50, vm.pet.value.mood)
        talk(vm)
        assertNotNull(vm.line.value)
        now += 1_000
        change(vm, "duck")
        assertEquals(70, vm.pet.value.mood)
        assertNull(vm.line.value)
        assertEquals("오리야", vm.species.value.callName)
        assertEquals(listOf(Species.DUCK), repo.savedSpecies)
        talk(vm)
        assertEquals("꽥! 안녕!", narrator.lines.last())
    }

    @Test
    fun change_to_the_same_species_changes_nothing() = runTest(dispatcher) {
        val repo = FakeRepo(initial = PetState(40, 1_000, 0, 0), initialSpecies = Species.CAT)
        val vm = newVm(repo)
        park(vm)
        change(vm, "cat")
        assertEquals(40, vm.pet.value.mood)
        assertTrue(repo.savedSpecies.isEmpty())
    }

    @Test
    fun change_is_ignored_while_driving() = runTest(dispatcher) {
        val vm = newVm(FakeRepo(initialSpecies = Species.CAT))
        change(vm, "dog")
        assertEquals(Species.CAT, vm.species.value)
    }

    @Test
    fun drive_start_after_choosing_uses_the_new_species() = runTest(dispatcher) {
        val vm = newVm()
        park(vm)
        choose(vm, "dog")
        park(vm, false)
        assertEquals(listOf("출발이다! 신난다!"), narrator.lines)
    }

    @Test
    fun talk_and_ask_use_the_chosen_species_lines() = runTest(dispatcher) {
        val bear = newVm(FakeRepo(initialSpecies = Species.BEAR))
        talk(bear)
        assertEquals("안녀엉~ 오늘도 든든하게 가자", narrator.lines.last())

        val cat = newVm(FakeRepo(initialSpecies = Species.CAT))
        cat.ask("오늘 날씨 어때?"); advanceUntilIdle()
        assertEquals("음? 무슨 말이냥", narrator.lines.last())
        cat.applyPreset(Preset.RUNNING_OUT); advanceUntilIdle()
        assertEquals(alertLine(Alert.FUEL_SHORT), narrator.lines.last())
    }
}
