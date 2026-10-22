package com.pleosplayground.drivequest.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pleosplayground.drivequest.core.DriveStyle
import com.pleosplayground.drivequest.core.Preset
import com.pleosplayground.drivequest.core.UxState
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
    var fuel by remember { mutableStateOf(80f) }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PetFace(face, Modifier.width(220.dp))
        Text(line ?: "…", style = MaterialTheme.typography.titleMedium)
        if (parked) {
            Gauge("밥", food)
            Gauge("건강", health)
            Gauge("기분", pet.mood)
            Button(onClick = vm::petIt) { Text("쓰다듬기") }
        }
        // ponytail: dev panel (fake signals) — remove before building against real signals.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("정차 중")
            Switch(checked = parked, onCheckedChange = vm::setParked)
            Button(onClick = vm::talk) { Text("말 걸기") }
        }
        if (parked) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Preset.entries.forEach { p ->
                    FilterChip(selected = false, onClick = { vm.applyPreset(p) }, label = { Text(p.name) })
                }
            }
            Text("연료 ${fuel.toInt()}%")
            Slider(
                value = fuel,
                onValueChange = { fuel = it; vm.setFuel(it.toInt()) },
                valueRange = 0f..100f,
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.Center) {
                DriveStyle.entries.forEach { d ->
                    FilterChip(selected = style == d, onClick = { style = d }, label = { Text(d.name) })
                }
                Button(onClick = { vm.startDrive(style) }) { Text("주행 시작") }
            }
        }
    }
}

@Composable
private fun Gauge(label: String, value: Int?) {
    if (value == null) return
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(48.dp))
        LinearProgressIndicator(progress = { value / 100f }, modifier = Modifier.weight(1f))
        Text("$value", Modifier.width(40.dp).padding(start = 8.dp))
    }
}
