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
    public override fun onCleared() { cleared = true }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ModeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm(store: FakeModeStore) = ModeViewModel(store).also { scope.advanceUntilIdle() }

    private fun probeIn(vm: ModeViewModel): Probe =
        ViewModelProvider(vm.petViewModels, viewModelFactory { initializer { Probe() } })["dev", Probe::class.java]

    @Test fun not_chosen_shows_the_start_screen() {
        val vm = vm(FakeModeStore())
        assertTrue(vm.loaded.value)
        assertNull(vm.mode.value)
    }

    @Test fun stored_mode_is_restored() {
        assertEquals(AppMode.CONNECTED, vm(FakeModeStore("connected")).mode.value)
        assertEquals(AppMode.DEV, vm(FakeModeStore("dev")).mode.value)
    }

    @Test fun nothing_is_shown_before_the_stored_mode_is_read() {
        val gate = CompletableDeferred<Unit>()
        val vm = vm(FakeModeStore("dev", gate = gate))
        assertFalse(vm.loaded.value)
        assertNull(vm.mode.value)
        gate.complete(Unit)
        scope.advanceUntilIdle()
        assertTrue(vm.loaded.value)
        assertEquals(AppMode.DEV, vm.mode.value)
    }

    @Test fun failed_load_shows_the_start_screen() {
        val vm = vm(FakeModeStore("dev", failLoad = true))
        assertTrue(vm.loaded.value)
        assertNull(vm.mode.value)
    }

    @Test fun unknown_stored_id_shows_the_start_screen() {
        for (id in listOf("DEV", "dev ", "", "garage")) {
            val vm = vm(FakeModeStore(id))
            assertTrue(id, vm.loaded.value)
            assertNull(id, vm.mode.value)
        }
    }

    @Test fun choose_persists_the_id() {
        val store = FakeModeStore()
        val vm = vm(store)
        vm.choose(AppMode.DEV)
        scope.advanceUntilIdle()
        assertEquals(AppMode.DEV, vm.mode.value)
        assertEquals("dev", store.stored)
    }

    @Test fun failed_save_keeps_the_choice() {
        val vm = vm(FakeModeStore(failSave = true))
        vm.choose(AppMode.CONNECTED)
        scope.advanceUntilIdle()
        assertEquals(AppMode.CONNECTED, vm.mode.value)
        vm.clear()
        scope.advanceUntilIdle()
        assertNull(vm.mode.value)
    }

    @Test fun clear_returns_to_the_start_screen() {
        val store = FakeModeStore("dev")
        val vm = vm(store)
        vm.clear()
        scope.advanceUntilIdle()
        assertNull(vm.mode.value)
        assertNull(store.stored)
        assertTrue(vm.loaded.value)
    }

    @Test fun choose_before_load_waits_for_it() {
        val gate = CompletableDeferred<Unit>()
        val store = FakeModeStore("connected", gate = gate)
        val vm = vm(store)
        vm.choose(AppMode.DEV)
        gate.complete(Unit)
        scope.advanceUntilIdle()
        assertEquals(AppMode.DEV, vm.mode.value)
        assertEquals("dev", store.stored)
    }

    @Test fun switching_mode_clears_the_pet_view_models() {
        val vm = vm(FakeModeStore("dev"))
        val probe = probeIn(vm)
        vm.choose(AppMode.CONNECTED)
        scope.advanceUntilIdle()
        assertTrue(probe.cleared)
    }

    @Test fun same_mode_keeps_them_and_clear_drops_them() {
        val vm = vm(FakeModeStore("dev"))
        val probe = probeIn(vm)
        vm.choose(AppMode.DEV)
        scope.advanceUntilIdle()
        assertFalse(probe.cleared)
        vm.clear()
        scope.advanceUntilIdle()
        assertTrue(probe.cleared)
    }
}
