package com.pleosplayground.drivequest.ui

import com.pleosplayground.drivequest.core.ConnectionState
import com.pleosplayground.drivequest.core.DriveStyle
import com.pleosplayground.drivequest.core.Face
import com.pleosplayground.drivequest.core.FakeSignalSource
import com.pleosplayground.drivequest.core.PetState
import com.pleosplayground.drivequest.core.Preset
import com.pleosplayground.drivequest.core.Species
import com.pleosplayground.drivequest.core.TripInfo
import com.pleosplayground.drivequest.core.UnavailableSignalSource
import com.pleosplayground.drivequest.core.UxState
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PetViewModelModeTest {
    private val dispatcher = StandardTestDispatcher()
    private var now = 1_000L
    private val narrator = FakeNarrator()
    private val noTrip = TripInfo(null, null, null, null)

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.connectedVm(repo: FakeRepo = FakeRepo(), n: FakeNarrator = narrator): PetViewModel {
        val vm = PetViewModel(UnavailableSignalSource(), null, repo, n) { now }
        advanceUntilIdle()
        return vm
    }

    @Test
    fun connected_starts_restricted_with_hidden_gauges() = runTest(dispatcher) {
        val vm = connectedVm()
        assertEquals(UxState.RESTRICTED, vm.ux.value)
        assertEquals(ConnectionState.UNAVAILABLE, vm.connection.value)
        assertNull(vm.food.value)
        assertNull(vm.health.value)
        assertEquals(noTrip, vm.trip.value)
        assertEquals(Face.HAPPY, vm.face.value)
        assertTrue(narrator.lines.isEmpty())
    }

    @Test
    fun connected_face_follows_mood_only() = runTest(dispatcher) {
        val vm = connectedVm(FakeRepo(initial = PetState(30, 1_000, 0, 0)))
        assertEquals(Face.SULKY, vm.face.value)
    }

    @Test
    fun connected_counts_as_driving() = runTest(dispatcher) {
        val repo = FakeRepo()
        val vm = connectedVm(repo)
        assertFalse(vm.needsPicker.value)
        vm.setParked(true); advanceUntilIdle()
        assertEquals(UxState.RESTRICTED, vm.ux.value)
        assertFalse(vm.needsPicker.value)
        vm.petIt(); advanceUntilIdle()
        assertEquals(70, vm.pet.value.mood)
        vm.chooseSpecies("cat"); advanceUntilIdle()
        assertEquals(Species.RABBIT, vm.species.value)
        assertFalse(vm.speciesChosen.value)
        assertTrue(repo.savedSpecies.isEmpty())
    }

    @Test
    fun dev_actions_do_nothing_without_dev_controls() = runTest(dispatcher) {
        val repo = FakeRepo()
        val vm = connectedVm(repo)
        val savedBefore = repo.saved.size
        vm.setParked(true)
        vm.setFuel(10)
        vm.applyPreset(Preset.LOW_FUEL)
        vm.setTrip(50f, "회사", 20f, 25)
        vm.startDrive(DriveStyle.HARSH)
        advanceUntilIdle()
        assertEquals(UxState.RESTRICTED, vm.ux.value)
        assertNull(vm.food.value)
        assertEquals(noTrip, vm.trip.value)
        assertEquals(70, vm.pet.value.mood)
        assertEquals(savedBefore, repo.saved.size)
        assertTrue(narrator.lines.isEmpty())
    }

    @Test
    fun connected_talk_speaks_the_rabbit_greeting() = runTest(dispatcher) {
        val vm = connectedVm()
        vm.talk(); advanceUntilIdle()
        assertEquals(listOf("안녕! 오늘도 같이 달려요"), narrator.lines)
        assertEquals("안녕! 오늘도 같이 달려요", vm.line.value)
        assertEquals(75, vm.pet.value.mood)
    }

    @Test
    fun fake_view_model_reports_connected() = runTest(dispatcher) {
        val vm = PetViewModel(FakeSignalSource(), FakeRepo(), FakeNarrator()) { now }
        advanceUntilIdle()
        assertEquals(ConnectionState.CONNECTED, vm.connection.value)
    }

    @Test
    fun two_modes_keep_separate_moods_but_share_the_species() = runTest(dispatcher) {
        val devRepo = FakeRepo(initialSpecies = Species.CAT)
        val connRepo = FakeRepo(initial = PetState(40, 1_000, 0, 0), initialSpecies = Species.CAT)
        val fake = FakeSignalSource()
        val dev = PetViewModel(fake, fake, devRepo, FakeNarrator()) { now }
        val conn = PetViewModel(UnavailableSignalSource(), null, connRepo, FakeNarrator()) { now }
        advanceUntilIdle()
        dev.setParked(true); dev.petIt(); advanceUntilIdle()
        assertEquals(80, devRepo.saved.last().mood)
        assertEquals(40, conn.pet.value.mood)
        assertTrue(connRepo.saved.all { it.mood == 40 })
        assertEquals(Species.CAT, dev.species.value)
        assertEquals(Species.CAT, conn.species.value)
    }
}
