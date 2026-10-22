package com.pleosplayground.drivequest.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pleosplayground.drivequest.core.Face

private val ShellTop = Color(0xFFFFC2DA)
private val ShellBottom = Color(0xFFFF93BC)
private val LcdColor = Color(0xFFF4EDD6)
private val LcdDot = Color(0xFFE2D9BC)
private val LcdEdge = Color(0xFF8C8566)
private val LcdInk = Color(0xFF2B2F44)

/** Filled hearts out of 4 (ceil(value/25), clamped); null when the gauge is unavailable. */
fun heartsFor(value: Int?): Int? = value?.let { (it.coerceIn(0, 100) + 24) / 25 }

@Composable
fun TamaDevice(
    face: Face, line: String?,
    food: Int?, health: Int?, mood: Int,
    parked: Boolean,
    onPet: () -> Unit, onTalk: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false, // landscape: LCD and buttons side by side so the device fits the height
) {
    val shape = if (compact) RoundedCornerShape(40.dp)
    else RoundedCornerShape(topStartPercent = 40, topEndPercent = 40, bottomStartPercent = 30, bottomEndPercent = 30)
    val buttons = @Composable {
        RoundButton("💬", "말 걸기", onTalk)
        if (parked) RoundButton("🤚", "쓰다듬기", onPet)
    }
    val base = modifier
        .shadow(14.dp, shape)
        .clip(shape)
        .background(Brush.verticalGradient(listOf(ShellTop, ShellBottom)))
    if (compact) {
        Row(base.padding(horizontal = 28.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Lcd(face, line, food, health, mood, parked, true, Modifier.weight(1f))
            Row(Modifier.padding(start = 24.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) { buttons() }
        }
    } else {
        Column(
            base.padding(horizontal = 26.dp).padding(top = 62.dp, bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Lcd(face, line, food, health, mood, parked, false)
            Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) { buttons() }
        }
    }
}

@Composable
private fun Lcd(
    face: Face, line: String?, food: Int?, health: Int?, mood: Int,
    parked: Boolean, compact: Boolean, modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(LcdColor)
            .dotGrid()
            .border(4.dp, LcdEdge, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Driving (restricted): only the face and one voice line, per the spec.
        if (parked) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatusIcon("🍙", "밥", food)
                StatusIcon("🩺", "건강", health)
                StatusIcon("😊", "기분", mood)
            }
        }
        PetFace(face, (if (compact) Modifier.height(96.dp) else Modifier.fillMaxWidth(0.8f)).padding(vertical = 8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = if (compact) 44.dp else 56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.75f))
                .border(2.dp, LcdEdge.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(line ?: "", color = LcdInk, fontSize = 15.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        }
    }
}

// Faint dot matrix plus an inset shadow along the top edge, drawn over the LCD fill.
private fun Modifier.dotGrid(): Modifier = drawBehind {
    val step = 7.dp.toPx()
    var y = step / 2
    while (y < size.height) {
        var x = step / 2
        while (x < size.width) {
            drawCircle(LcdDot, 1.1.dp.toPx(), Offset(x, y))
            x += step
        }
        y += step
    }
    drawRect(Brush.verticalGradient(listOf(Color(0x33000000), Color.Transparent), endY = 22.dp.toPx()))
}

@Composable
private fun StatusIcon(emoji: String, label: String, value: Int?) {
    val hearts = heartsFor(value) ?: return
    Column(
        Modifier.semantics(mergeDescendants = true) { contentDescription = "$label ${hearts}/4칸" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 22.sp)
        Row(Modifier.clearAndSetSemantics {}) {
            repeat(4) { i -> Text("❤️", Modifier.alpha(if (i < hearts) 1f else 0.18f), fontSize = 14.sp) }
        }
    }
}

@Composable
private fun RoundButton(emoji: String, label: String, onClick: () -> Unit) {
    Column(
        Modifier.clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .shadow(6.dp, CircleShape)
                .background(Brush.verticalGradient(listOf(Color(0xFFFFF3C4), Color(0xFFFFD36B))), CircleShape)
                .border(2.dp, Color(0xFFE0A93A), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text(emoji, fontSize = 28.sp) }
        Text(label, Modifier.padding(top = 6.dp), color = Color(0xFF5A1F3A), fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
