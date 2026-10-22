package com.pleosplayground.drivequest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pleosplayground.drivequest.core.Face

// Palette from the "B. 포근한 내 방" mockup.
internal val Line = Color(0xFF6B5648)
private val WallFill = Color(0xFFFFF1D6)
private val WallDot = Color(0xFFFFE3B8)
private val FloorFill = Color(0xFFF1CF9F)
private val FloorPlank = Color(0xFFE8C08C)
private val SkyFill = Color(0xFFD9EFFF)
private val RugFill = Color(0xFFFFD0DC)
private val Yellow = Color(0xFFFFE8A3)
private val Pink = Color(0xFFFFC9D6)
private val HeartOn = Color(0xFFFF8FA8)
private val HeartOff = Color(0xFFE8D5D0)

/** Filled hearts out of 4 (ceil(value/25), clamped); null when the gauge is unavailable. */
fun heartsFor(value: Int?): Int? = value?.let { (it.coerceIn(0, 100) + 24) / 25 }

/** A cozy room: wall, window, floor, rug, the pet sitting on it, a speech bubble, a heart sign and pill buttons. */
@Composable
fun RoomScene(
    face: Face, line: String?,
    food: Int?, health: Int?,
    parked: Boolean,
    onPet: () -> Unit, onTalk: () -> Unit,
    modifier: Modifier = Modifier,
    mood: Int? = null, // null hides the mood row; not in the brief's signature but the sign needs it
) {
    val shape = RoundedCornerShape(28.dp)
    BoxWithConstraints(modifier.clip(shape).roomBackdrop().border(3.dp, Line, shape)) {
        val wide = maxWidth > maxHeight
        val petH = maxHeight * 0.40f
        // Pet sits on the rug: its wheels end a bit below the rug's centre line.
        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = maxHeight * 0.255f)
                .widthIn(max = maxWidth * 0.7f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Bubble(line)
            PetFace(face, Modifier.height(petH))
        }
        if (parked) {
            Column(
                Modifier.align(Alignment.TopEnd).padding(14.dp)
                    .background(Color.White, RoundedCornerShape(18.dp))
                    .border(3.dp, Line, RoundedCornerShape(18.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                SignRow("🍙", "밥", food)
                SignRow("🩺", "건강", health)
                SignRow("😊", "기분", mood)
            }
        }
        Row(
            Modifier.align(if (wide) Alignment.BottomEnd else Alignment.BottomCenter).padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (parked) PillButton("🤚 쓰다듬기", Pink, onPet)
            PillButton("💬 말 걸기", Yellow, onTalk)
        }
    }
}

/** Dotted cream wall, sky window, plank floor and pink rug, all in the thin brown outline. */
private fun Modifier.roomBackdrop(): Modifier = drawBehind {
    val lw = 3.dp.toPx()
    val stroke = Stroke(lw)
    val floorY = size.height * 0.60f
    drawRect(WallFill, Offset.Zero, Size(size.width, floorY))
    val step = 16.dp.toPx()
    var y = step / 2
    while (y < floorY) {
        var x = step / 2
        while (x < size.width) { drawCircle(WallDot, 1.6.dp.toPx(), Offset(x, y)); x += step }
        y += step
    }
    drawRect(FloorFill, Offset(0f, floorY), Size(size.width, size.height - floorY))
    var px = 49.dp.toPx()
    while (px < size.width) { drawLine(FloorPlank, Offset(px, floorY), Offset(px, size.height), lw); px += 49.dp.toPx() }
    drawLine(Line, Offset(0f, floorY), Offset(size.width, floorY), lw)
    // window with cross muntins
    val ww = (size.width * 0.14f).coerceIn(74.dp.toPx(), 110.dp.toPx())
    val wh = ww * 0.86f
    val wx = 22.dp.toPx(); val wy = 24.dp.toPx()
    val r = CornerRadius(14.dp.toPx())
    drawRoundRect(SkyFill, Offset(wx, wy), Size(ww, wh), r)
    drawLine(Line, Offset(wx + ww / 2, wy), Offset(wx + ww / 2, wy + wh), lw)
    drawLine(Line, Offset(wx, wy + wh / 2), Offset(wx + ww, wy + wh / 2), lw)
    drawRoundRect(Line, Offset(wx, wy), Size(ww, wh), r, style = stroke)
    // rug
    val rw = (size.width * 0.62f).coerceAtMost(260.dp.toPx())
    val rh = size.height * 0.12f
    val rx = (size.width - rw) / 2
    val ry = size.height * 0.74f - rh / 2
    drawOval(RugFill, Offset(rx, ry), Size(rw, rh))
    drawOval(Line, Offset(rx, ry), Size(rw, rh), style = stroke)
}

@Composable
private fun Bubble(line: String?) {
    if (line.isNullOrEmpty()) return
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            line,
            Modifier
                .background(Color.White, RoundedCornerShape(18.dp))
                .border(3.dp, Line, RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            color = Line, fontSize = 15.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
        )
        // tail pointing down at the pet
        Box(
            Modifier.padding(bottom = 4.dp).height(10.dp).widthIn(min = 20.dp).drawBehind {
                val w = 8.dp.toPx(); val cx = size.width / 2
                val p = Path().apply { moveTo(cx - w, 0f); lineTo(cx, size.height); lineTo(cx + w, 0f) }
                drawPath(p, Color.White)
                drawPath(p, Line, style = Stroke(3.dp.toPx()))
                drawLine(Color.White, Offset(cx - w + 1.5.dp.toPx(), 0f), Offset(cx + w - 1.5.dp.toPx(), 0f), 4.dp.toPx())
            },
        )
    }
}

@Composable
private fun SignRow(emoji: String, label: String, value: Int?) {
    val hearts = heartsFor(value) ?: return
    Row(
        Modifier.semantics(mergeDescendants = true) { contentDescription = "$label ${hearts}/4칸" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(emoji, Modifier.clearAndSetSemantics {}, fontSize = 16.sp)
        Row(Modifier.clearAndSetSemantics {}) {
            repeat(4) { i -> Text("♥\uFE0E", color = if (i < hearts) HeartOn else HeartOff, fontSize = 17.sp) }
        }
    }
}

@Composable
private fun PillButton(label: String, fill: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        Modifier
            .drawBehind { // hard bottom shadow, like the mockup's 0 3px 0
                drawRoundRect(Line, Offset(0f, 3.dp.toPx()), size, CornerRadius(24.dp.toPx()))
            }
            .background(fill, shape)
            .border(3.dp, Line, shape)
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Line, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
}
