package com.pleosplayground.drivequest.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pleosplayground.drivequest.core.AppMode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class Probe : ViewModel() {
    var cleared = false
    var onClear: () -> Unit = {}
    public override fun onCleared() { cleared = true; onClear() }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ModeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.vm(store: FakeModeStore) = ModeViewModel(store).also { advanceUntilIdle() }

    private fun probeIn(vm: ModeViewModel): Probe =
        ViewModelProvider(vm.petViewModels, viewModelFactory { initializer { Probe() } })["dev", Probe::class.java]

    @Test fun not_chosen_shows_the_start_screen() = runTest(dispatcher) {
        val vm = vm(FakeModeStore())
        assertTrue(vm.loaded.value)
        assertNull(vm.mode.value)
    }

    @Test fun stored_mode_is_restored() = runTest(dispatcher) {
        assertEquals(AppMode.CONNECTED, vm(FakeModeStore("connected")).mode.value)
        assertEquals(AppMode.DEV, vm(FakeModeStore("dev")).mode.value)
    }

    @Test fun nothing_is_shown_before_the_stored_mode_is_read() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val vm = vm(FakeModeStore("dev", gate = gate))
        assertFalse(vm.loaded.value)
        assertNull(vm.mode.value)
        gate.complete(Unit)
        advanceUntilIdle()
        assertTrue(vm.loaded.value)
        assertEquals(AppMode.DEV, vm.mode.value)
    }

    @Test fun failed_load_shows_the_start_screen() = runTest(dispatcher) {
        val vm = vm(FakeModeStore("dev", failLoad = true))
        assertTrue(vm.loaded.value)
        assertNull(vm.mode.value)
    }

    @Test fun unknown_stored_id_shows_the_start_screen() = runTest(dispatcher) {
        for (id in listOf("DEV", "dev ", "", "garage")) {
            val vm = vm(FakeModeStore(id))
            assertTrue(id, vm.loaded.value)
            assertNull(id, vm.mode.value)
        }
    }

    @Test fun choose_persists_the_id() = runTest(dispatcher) {
        val store = FakeModeStore()
        val vm = vm(store)
        vm.choose(AppMode.DEV)
        advanceUntilIdle()
        assertEquals(AppMode.DEV, vm.mode.value)
        assertEquals("dev", store.stored)
    }

    @Test fun failed_save_keeps_the_choice() = runTest(dispatcher) {
        val vm = vm(FakeModeStore(failSave = true))
        vm.choose(AppMode.CONNECTED)
        advanceUntilIdle()
        assertEquals(AppMode.CONNECTED, vm.mode.value)
        vm.clear()
        advanceUntilIdle()
        assertNull(vm.mode.value)
    }

    @Test fun clear_returns_to_the_start_screen() = runTest(dispatcher) {
        val store = FakeModeStore("dev")
        val vm = vm(store)
        vm.clear()
        advanceUntilIdle()
        assertNull(vm.mode.value)
        assertNull(store.stored)
        assertTrue(vm.loaded.value)
    }

    @Test fun choose_before_load_waits_for_it() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val store = FakeModeStore("connected", gate = gate)
        val vm = vm(store)
        vm.choose(AppMode.DEV)
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(AppMode.DEV, vm.mode.value)
        assertEquals("dev", store.stored)
    }

    @Test fun switching_mode_clears_the_pet_view_models() = runTest(dispatcher) {
        val vm = vm(FakeModeStore("dev"))
        val probe = probeIn(vm)
        vm.choose(AppMode.CONNECTED)
        advanceUntilIdle()
        assertTrue(probe.cleared)
    }

    @Test fun store_is_cleared_before_the_new_mode_is_published() = runTest(dispatcher) {
        val vm = vm(FakeModeStore("dev"))
        val probe = probeIn(vm)
        var modeWhenCleared: AppMode? = null
        probe.onClear = { modeWhenCleared = vm.mode.value }
        vm.choose(AppMode.CONNECTED)
        advanceUntilIdle()
        assertEquals(AppMode.DEV, modeWhenCleared)
    }

    @Test fun same_mode_keeps_them_and_clear_drops_them() = runTest(dispatcher) {
        val vm = vm(FakeModeStore("dev"))
        val probe = probeIn(vm)
        vm.choose(AppMode.DEV)
        advanceUntilIdle()
        assertFalse(probe.cleared)
        vm.clear()
        advanceUntilIdle()
        assertTrue(probe.cleared)
    }
}
