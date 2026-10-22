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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pleosplayground.drivequest.core.DriveStyle
import com.pleosplayground.drivequest.core.Preset
import com.pleosplayground.drivequest.core.canPlay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PetScreen(vm: PetViewModel) {
    val face by vm.face.collectAsState()
    val ux by vm.ux.collectAsState()
    val line by vm.line.collectAsState()
    val food by vm.food.collectAsState()
    val health by vm.health.collectAsState()
    val pet by vm.pet.collectAsState()
    val parked = canPlay(ux)
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
            RoomScene(
                face, line, food, health, parked, vm::petIt, vm::talk,
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
            if (parked) {
                Column(
                    Modifier.widthIn(max = 480.dp).fillMaxWidth()
                        .background(Color(0xFFFFF7E8), RoundedCornerShape(22.dp))
                        .border(3.dp, Line, RoundedCornerShape(22.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("개발자 도구 · 기분 ${pet.mood}", color = Line, style = MaterialTheme.typography.labelMedium)
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
}

private val Cream = Color(0xFFFFF1D6)
private val SwitchRowHeight = 48.dp
