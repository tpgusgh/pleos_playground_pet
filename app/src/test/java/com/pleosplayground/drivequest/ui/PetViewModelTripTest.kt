package com.pleosplayground.drivequest.ui

import com.pleosplayground.drivequest.core.ALERT_GAP_MS
import com.pleosplayground.drivequest.core.Alert
import com.pleosplayground.drivequest.core.Face
import com.pleosplayground.drivequest.core.FakeSignalSource
import com.pleosplayground.drivequest.core.Preset
import com.pleosplayground.drivequest.core.TripInfo
import com.pleosplayground.drivequest.core.alertLine
import com.pleosplayground.drivequest.core.driveStartLine
import com.pleosplayground.drivequest.core.notUnderstoodLine
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PetViewModelTripTest {
    private val dispatcher = StandardTestDispatcher()
    private var now = 1_000L
    private val narrator = FakeNarrator()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.newVm(): PetViewModel {
        val vm = PetViewModel(FakeSignalSource(), FakeRepo(), narrator) { now }
        advanceUntilIdle()
        return vm
    }

    private fun TestScope.preset(vm: PetViewModel, p: Preset) { vm.applyPreset(p); advanceUntilIdle() }
    private fun TestScope.ask(vm: PetViewModel, q: String) { vm.ask(q); advanceUntilIdle() }

    @Test
    fun trip_follows_the_signal_source() = runTest(dispatcher) {
        val vm = newVm()
        preset(vm, Preset.LONG_TRIP)
        assertEquals(TripInfo(200f, "집", 80f, 70), vm.trip.value)
    }

    @Test
    fun ask_range_answers_from_trip_info() = runTest(dispatcher) {
        val vm = newVm()
        preset(vm, Preset.LONG_TRIP)
        ask(vm, "연료 얼마나 가?")
        assertEquals(listOf("지금 연료로 약 200킬로미터 갈 수 있어요"), narrator.lines)
    }

    @Test
    fun ask_destination_without_destination_says_not_set() = runTest(dispatcher) {
        val vm = newVm()
        ask(vm, "목적지까지 얼마나 남았어?")
        assertEquals(listOf("목적지가 설정 안 돼 있어요"), narrator.lines)
    }

    @Test
    fun ask_enough_follows_the_verdict() = runTest(dispatcher) {
        val vm = newVm()
        preset(vm, Preset.RUNNING_OUT)
        ask(vm, "목적지까지 연료 충분해?")
        assertEquals("아니요, 목적지까지 연료가 모자랄 것 같아요", narrator.lines.last())
    }

    @Test
    fun ask_unrecognized_says_not_understood() = runTest(dispatcher) {
        val vm = newVm()
        ask(vm, "오늘 날씨 어때?")
        assertEquals(listOf(notUnderstoodLine(Face.HAPPY)), narrator.lines)
    }

    @Test
    fun ask_works_while_driving() = runTest(dispatcher) {
        val vm = newVm()
        ask(vm, "연료 얼마나 가?")
        assertEquals(1, narrator.lines.size)
    }

    @Test
    fun ask_respects_the_talk_cooldown() = runTest(dispatcher) {
        val vm = newVm()
        vm.ask("연료 얼마나 가?")
        vm.ask("연료 얼마나 가?")
        advanceUntilIdle()
        assertEquals(1, narrator.lines.size)
        assertEquals(75, vm.pet.value.mood)
        vm.talk()
        advanceUntilIdle()
        assertEquals(1, narrator.lines.size)
    }

    @Test
    fun ask_after_talk_in_same_cooldown_is_silent() = runTest(dispatcher) {
        val vm = newVm()
        vm.talk(); advanceUntilIdle()
        vm.ask("연료 얼마나 가?"); advanceUntilIdle()
        assertEquals(1, narrator.lines.size)
        assertEquals(75, vm.pet.value.mood)
    }

    @Test
    fun running_out_speaks_fuel_short_once() = runTest(dispatcher) {
        val vm = newVm()
        preset(vm, Preset.RUNNING_OUT)
        assertEquals(listOf(alertLine(Alert.FUEL_SHORT)), narrator.lines)
        now += ALERT_GAP_MS * 3
        vm.setTrip(40f, "집", 119f, 99); advanceUntilIdle()
        assertEquals(listOf(alertLine(Alert.FUEL_SHORT)), narrator.lines)
    }

    @Test
    fun nearly_there_speaks() = runTest(dispatcher) {
        val vm = newVm()
        preset(vm, Preset.LONG_TRIP)
        vm.setTrip(200f, "집", 0.8f, 1); advanceUntilIdle()
        assertEquals(listOf(alertLine(Alert.NEARLY_THERE)), narrator.lines)
    }

    @Test
    fun alerts_are_silent_when_disabled_and_speak_when_enabled() = runTest(dispatcher) {
        val vm = newVm()
        vm.setProactive(false); advanceUntilIdle()
        preset(vm, Preset.RUNNING_OUT)
        assertEquals(emptyList<String>(), narrator.lines)
        vm.setProactive(true); advanceUntilIdle()
        assertEquals(listOf(alertLine(Alert.FUEL_SHORT)), narrator.lines)
    }

    @Test
    fun alert_gap_is_shared_with_the_drive_start_greeting() = runTest(dispatcher) {
        val vm = newVm()
        vm.setParked(true); advanceUntilIdle()
        vm.setParked(false); advanceUntilIdle()
        assertEquals(listOf(driveStartLine(Face.HAPPY)), narrator.lines)
        preset(vm, Preset.RUNNING_OUT)
        assertEquals(1, narrator.lines.size)
        now += ALERT_GAP_MS
        vm.setTrip(40f, "집", 120f, 101); advanceUntilIdle()
        assertEquals(alertLine(Alert.FUEL_SHORT), narrator.lines.last())
        assertEquals(2, narrator.lines.size)
    }

    @Test
    fun drive_start_greeting_is_skipped_right_after_an_alert() = runTest(dispatcher) {
        val vm = newVm()
        preset(vm, Preset.RUNNING_OUT)
        assertEquals(listOf(alertLine(Alert.FUEL_SHORT)), narrator.lines)
        vm.setParked(true); advanceUntilIdle()
        vm.setParked(false); advanceUntilIdle()
        assertEquals(1, narrator.lines.size)
    }

    @Test
    fun missing_values_do_not_break_alerts_or_answers() = runTest(dispatcher) {
        val vm = newVm()
        preset(vm, Preset.UNSUPPORTED)
        ask(vm, "목적지까지 연료 충분해?")
        assertEquals(listOf("목적지가 설정 안 돼 있어요"), narrator.lines)
    }
}
