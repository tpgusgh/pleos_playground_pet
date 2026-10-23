package com.pleosplayground.drivequest.ui

import com.pleosplayground.drivequest.core.DriveStyle
import com.pleosplayground.drivequest.core.FakeSignalSource
import com.pleosplayground.drivequest.core.Face
import com.pleosplayground.drivequest.core.PetState
import com.pleosplayground.drivequest.core.Preset
import com.pleosplayground.drivequest.core.driveStartLine
import com.pleosplayground.drivequest.core.greeting
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PetViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private var now = 1_000L

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm(
        repo: FakeRepo = FakeRepo(),
        narrator: FakeNarrator = FakeNarrator(),
    ) = PetViewModel(FakeSignalSource(), repo, narrator) { now }

    @Test
    fun load_failure_falls_back_to_default_pet() = runTest(dispatcher) {
        val vm = vm(FakeRepo(failLoad = true))
        advanceUntilIdle()
        assertEquals(70, vm.pet.value.mood)
    }

    @Test
    fun save_failure_does_not_crash() = runTest(dispatcher) {
        val vm = vm(FakeRepo(failSave = true))
        advanceUntilIdle()
        vm.setParked(true)
        advanceUntilIdle()
        vm.petIt()
        advanceUntilIdle()
        assertEquals(80, vm.pet.value.mood)
    }

    @Test
    fun open_applies_time_decay() = runTest(dispatcher) {
        now = 1_000 + 4 * 3_600_000L
        val vm = vm(FakeRepo(initial = PetState(70, 1_000, 0, 0)))
        advanceUntilIdle()
        assertEquals(62, vm.pet.value.mood)
    }

    @Test
    fun actions_wait_for_initial_load() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repo = FakeRepo(initial = PetState(40, 0, 0, 0), gate = gate)
        val vm = vm(repo)
        vm.setParked(true)
        vm.petIt()
        advanceUntilIdle()
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(50, repo.saved.last().mood)
        assertEquals(50, vm.pet.value.mood)
    }

    @Test
    fun pet_is_ignored_while_restricted() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.petIt()
        advanceUntilIdle()
        assertEquals(70, vm.pet.value.mood)
    }

    @Test
    fun pet_raises_mood_when_parked_and_respects_cooldown() = runTest(dispatcher) {
        val vm = vm()
        vm.setParked(true)
        advanceUntilIdle()
        vm.petIt()
        vm.petIt()
        advanceUntilIdle()
        assertEquals(80, vm.pet.value.mood)
    }

    @Test
    fun talk_replies_while_driving() = runTest(dispatcher) {
        val narrator = FakeNarrator()
        val vm = vm(narrator = narrator)
        advanceUntilIdle()
        vm.talk()
        advanceUntilIdle()
        assertEquals(listOf(greeting(Face.HAPPY)), narrator.lines)
        assertEquals(75, vm.pet.value.mood)
    }

    @Test
    fun talk_cooldown_blocks_repeat() = runTest(dispatcher) {
        val narrator = FakeNarrator()
        val vm = vm(narrator = narrator)
        advanceUntilIdle()
        vm.talk()
        vm.talk()
        advanceUntilIdle()
        assertEquals(1, narrator.lines.size)
    }

    private fun TestScope.cycle(vm: PetViewModel) {
        vm.setParked(true); advanceUntilIdle()
        vm.setParked(false); advanceUntilIdle()
    }

    @Test
    fun driving_start_greets_once_within_gap() = runTest(dispatcher) {
        val narrator = FakeNarrator()
        val vm = vm(narrator = narrator)
        advanceUntilIdle()
        assertEquals(emptyList<String>(), narrator.lines) // initial RESTRICTED is not a start
        val expected = driveStartLine(Face.HAPPY)
        cycle(vm)
        assertEquals(listOf(expected), narrator.lines)
        cycle(vm)
        assertEquals(1, narrator.lines.size)
        now += 600_000
        cycle(vm)
        assertEquals(2, narrator.lines.size)
        assertEquals(expected, vm.line.value)
    }

    @Test
    fun harsh_drive_lowers_mood_and_smooth_drive_raises_it() = runTest(dispatcher) {
        val harsh = vm()
        advanceUntilIdle()
        harsh.startDrive(DriveStyle.HARSH)
        advanceUntilIdle()
        assertTrue(harsh.pet.value.mood < 70)
        val smooth = vm()
        advanceUntilIdle()
        smooth.startDrive(DriveStyle.SMOOTH)
        advanceUntilIdle()
        assertTrue(smooth.pet.value.mood > 70)
    }

    @Test
    fun low_fuel_shows_hungry_face() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.setFuel(10)
        advanceUntilIdle()
        assertEquals(Face.HUNGRY, vm.face.value)
        assertEquals(10, vm.food.value)
    }

    @Test
    fun needs_care_shows_sulky_face() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.applyPreset(Preset.NEEDS_CARE)
        advanceUntilIdle()
        assertEquals(25, vm.health.value)
        assertEquals(Face.SULKY, vm.face.value)
    }

    @Test
    fun unsupported_hides_gauges_and_keeps_going() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.applyPreset(Preset.UNSUPPORTED)
        advanceUntilIdle()
        assertNull(vm.food.value)
        assertNull(vm.health.value)
        assertEquals(Face.HAPPY, vm.face.value)
    }
}
