package com.pleosplayground.drivequest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import kotlin.random.Random
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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

    NightBackground {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TamaDevice(
                face, line, food, health, pet.mood, parked, vm::petIt, vm::talk,
                Modifier.widthIn(max = 340.dp).fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (parked) "정차" else "주행 중", color = Ink, fontWeight = FontWeight.Bold)
                Switch(checked = parked, onCheckedChange = vm::setParked)
            }
            // ponytail: dev panel (fake signals) — remove before building against real signals.
            if (parked) {
                Column(
                    Modifier.widthIn(max = 340.dp).fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("개발자 도구 · 기분 ${pet.mood}", color = Ink, style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Preset.entries.forEach { p ->
                            FilterChip(selected = false, onClick = { vm.applyPreset(p) }, label = { Text(p.name) })
                        }
                    }
                    // food mirrors fuelPercent 1:1, so the slider has no state of its own.
                    Text(if (food == null) "연료 없음" else "연료 $food%", color = Ink)
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
                        Button(onClick = { vm.startDrive(style) }) { Text("주행 시작") }
                    }
                }
            }
        }
    }
}

private val Ink = Color(0xFFE8ECFA)
private val NavyTop = Color(0xFF1D2333)
private val NavyBottom = Color(0xFF2B3350)

/** Dark navy gradient with a faint, fixed-seed starfield. */
@Composable
fun NightBackground(content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(NavyTop, NavyBottom))).drawBehind {
            val rnd = Random(7)
            repeat(70) {
                drawCircle(
                    Color.White.copy(alpha = 0.15f + rnd.nextFloat() * 0.35f),
                    (0.6f + rnd.nextFloat() * 1.4f) * density,
                    Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height),
                )
            }
        },
    ) { content() }
}
