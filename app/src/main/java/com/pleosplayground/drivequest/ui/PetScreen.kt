package com.pleosplayground.drivequest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pleosplayground.drivequest.core.DriveStyle
import com.pleosplayground.drivequest.core.Preset
import com.pleosplayground.drivequest.core.Species
import com.pleosplayground.drivequest.core.canPlay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PetScreen(vm: PetViewModel) {
    val face by vm.face.collectAsState()
    val species by vm.species.collectAsState()
    val ux by vm.ux.collectAsState()
    val line by vm.line.collectAsState()
    val food by vm.food.collectAsState()
    val health by vm.health.collectAsState()
    val pet by vm.pet.collectAsState()
    val trip by vm.trip.collectAsState()
    val proactive by vm.proactiveEnabled.collectAsState()
    val needsPicker by vm.needsPicker.collectAsState()
    val parked = canPlay(ux)
    var changing by rememberSaveable { mutableStateOf(false) }
    var pendingChange by remember { mutableStateOf<Species?>(null) }
    val showPicker = needsPicker || (changing && parked)
    // Leaving parked mode must not leave the change screen or its dialog behind.
    LaunchedEffect(parked) { if (!parked) { changing = false; pendingChange = null } }
    var typed by rememberSaveable { mutableStateOf("") }
    var style by remember { mutableStateOf(DriveStyle.SMOOTH) }

    BoxWithConstraints(Modifier.fillMaxSize().background(Cream).safeDrawingPadding()) {
        val landscape = maxWidth > maxHeight
        // Landscape: the room takes the height left over (16dp padding twice, 8dp gap, switch row), so the
        // driving-mode screen needs no scroll; parked dev tools scroll below it. Portrait: room on top.
        val roomHeight = if (landscape) (maxHeight - 32.dp - 8.dp - SwitchRowHeight).coerceAtLeast(220.dp)
        else (maxWidth * 0.95f).coerceAtMost(maxHeight - 32.dp - 8.dp - SwitchRowHeight).coerceAtLeast(300.dp)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (showPicker) {
                SpeciesPicker(
                    initial = if (needsPicker) null else species,
                    onConfirm = {
                        if (needsPicker) vm.chooseSpecies(it.id)
                        else if (it == species) changing = false else pendingChange = it
                    },
                    onClose = if (needsPicker) null else ({ changing = false }),
                    modifier = Modifier.widthIn(max = if (landscape) 760.dp else 460.dp).fillMaxWidth(),
                    landscape = landscape,
                )
            } else RoomScene(
                face, species, line, food, health, parked, vm::petIt, vm::talk,
                Modifier.widthIn(max = if (landscape) 720.dp else 480.dp).fillMaxWidth().height(roomHeight),
                mood = pet.mood,
            )
            Row(
                Modifier.height(SwitchRowHeight),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(if (parked) "정차" else "주행 중", color = Line, fontWeight = FontWeight.Bold)
                Switch(checked = parked, onCheckedChange = vm::setParked)
            }
            // ponytail: dev panel (fake signals) — remove before building against real signals.
            if (parked && !showPicker) {
                Column(
                    Modifier.widthIn(max = 480.dp).fillMaxWidth()
                        .background(Color(0xFFFFF7E8), RoundedCornerShape(22.dp))
                        .border(3.dp, Line, RoundedCornerShape(22.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("개발자 도구 · 기분 ${pet.mood}", color = Line, style = MaterialTheme.typography.labelMedium)
                    // ponytail: 설정 대화상자가 생기면(LLM 계획) 그쪽으로 옮긴다
                    Button(
                        onClick = { changing = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Line, contentColor = Cream),
                    ) { Text("친구 바꾸기") }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Preset.entries.forEach { p ->
                            FilterChip(selected = false, onClick = { vm.applyPreset(p) }, label = { Text(p.name) })
                        }
                    }
                    // food mirrors fuelPercent 1:1, so the slider has no state of its own.
                    Text(if (food == null) "연료 없음" else "연료 $food%", color = Line)
                    Slider(
                        value = food?.toFloat() ?: 0f,
                        enabled = food != null,
                        onValueChange = { vm.setFuel(it.toInt()) },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    // Trip sliders derive from trip (no local copy); a null value disables them.
                    val range = trip.rangeKm
                    Text(if (range == null) "갈 수 있는 거리 없음" else "갈 수 있는 거리 ${range.toInt()}km", color = Line)
                    Slider(
                        value = range ?: 0f,
                        enabled = range != null,
                        onValueChange = { vm.setTrip(it, trip.destinationName, trip.distanceKm, trip.etaMinutes) },
                        valueRange = 0f..600f,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val dist = trip.distanceKm
                    Text(if (dist == null) "남은 거리 없음" else "남은 거리 ${dist.toInt()}km", color = Line)
                    Slider(
                        value = dist ?: 0f,
                        enabled = dist != null,
                        onValueChange = { vm.setTrip(trip.rangeKm, trip.destinationName, it, trip.etaMinutes) },
                        valueRange = 0f..300f,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("먼저 말하기", color = Line)
                        Switch(checked = proactive, onCheckedChange = vm::setProactive)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = typed,
                            onValueChange = { typed = it },
                            label = { Text("말 입력") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            onClick = { vm.ask(typed); typed = "" },
                            modifier = Modifier.heightIn(min = 48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Line, contentColor = Cream),
                        ) { Text("보내기") }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.Center) {
                        DriveStyle.entries.forEach { d ->
                            FilterChip(selected = style == d, onClick = { style = d }, label = { Text(d.name) })
                        }
                        Button(
                            onClick = { vm.startDrive(style) },
                            colors = ButtonDefaults.buttonColors(containerColor = Line, contentColor = Cream),
                        ) { Text("주행 시작") }
                    }
                }
            }
        }
    }
    pendingChange?.let { next ->
        AlertDialog(
            onDismissRequest = { pendingChange = null },
            containerColor = Color(0xFFFFF7E8),
            titleContentColor = Line,
            textContentColor = Line,
            shape = RoundedCornerShape(22.dp),
            title = { Text("친구를 바꿀까요?", fontWeight = FontWeight.Bold) },
            text = { Text("기분이 처음부터 다시 시작돼요") },
            confirmButton = {
                TextButton(colors = ButtonDefaults.textButtonColors(contentColor = Line), onClick = { vm.changeSpecies(next.id); pendingChange = null; changing = false }) { Text("바꾸기") }
            },
            dismissButton = {
                TextButton(colors = ButtonDefaults.textButtonColors(contentColor = Line), onClick = { pendingChange = null }) { Text("취소") }
            },
        )
    }
}

private val Cream = Color(0xFFFFF1D6)
private val SwitchRowHeight = 48.dp
