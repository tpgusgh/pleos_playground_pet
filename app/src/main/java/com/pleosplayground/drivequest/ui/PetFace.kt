package com.pleosplayground.drivequest.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.pleosplayground.drivequest.core.Face

private val Mint = Color(0xFF8FE3C8)
private val MintDark = Color(0xFF75D0B4)
private val Ink = Color(0xFF2B2F44)
private val Cheek = Color(0xFFFF9FB8)

// Drawn in the mockup's 150x112 design units (130x104 car plus margin for ears), scaled to fit the canvas.
@Composable
fun PetFace(face: Face, modifier: Modifier = Modifier) {
    val desc = when (face) {
        Face.HAPPY -> "행복한 차 캐릭터"
        Face.SULKY -> "시무룩한 차 캐릭터"
        Face.HUNGRY -> "배고픈 차 캐릭터"
    }
    Canvas(modifier.aspectRatio(150f / 112f).semantics { contentDescription = desc }) {
        scale(size.width / 150f, pivot = Offset.Zero) { translate(10f, 10f) { drawCar(face) } }
    }
}

private fun DrawScope.drawCar(face: Face) {
    val sulky = face == Face.SULKY
    // ears (side mirrors), hinged at their base
    val earTilt = if (sulky) 50f else 12f
    ear(20f, -earTilt)
    ear(90f, earTilt)
    // body
    val body = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                0f, 14f, 130f, 94f,
                CornerRadius(60f, 37f), CornerRadius(60f, 37f), CornerRadius(42f, 26f), CornerRadius(42f, 26f),
            ),
        )
    }
    drawPath(body, Mint)
    drawRoundRect(MintDark, Offset(14f, 85f), Size(102f, 9f), CornerRadius(4f))
    // eyes
    for (x in listOf(26f, 74f)) {
        drawOval(Color.White, Offset(x, 30f), Size(30f, 38f))
        val py = if (sulky) 44f else 38f
        drawOval(Ink, Offset(x + 7f, py), Size(17f, 21f))
        drawCircle(Color.White, 3.5f, Offset(x + 13f, py + 6f))
        drawCircle(Color.White, 2f, Offset(x + 20f, py + 14f))
    }
    if (sulky) {
        // brows droop outward
        drawLine(Ink, Offset(23f, 33f), Offset(53f, 26f), 5f, StrokeCap.Round)
        drawLine(Ink, Offset(107f, 33f), Offset(77f, 26f), 5f, StrokeCap.Round)
    }
    // cheeks
    val ca = if (sulky) 0.25f else 0.85f
    drawOval(Cheek.copy(alpha = ca), Offset(12f, 68f), Size(17f, 9f))
    drawOval(Cheek.copy(alpha = ca), Offset(101f, 68f), Size(17f, 9f))
    // mouth
    val line = Stroke(3f, cap = StrokeCap.Round)
    when (face) {
        Face.HAPPY -> drawArc(Ink, 0f, 180f, false, Offset(50f, 66f), Size(30f, 20f), style = line)
        Face.SULKY -> drawArc(Ink, 180f, 180f, false, Offset(53f, 80f), Size(24f, 14f), style = line)
        Face.HUNGRY -> {
            drawOval(Color(0xFFE56B7F), Offset(56f, 72f), Size(18f, 19f))
            drawOval(Ink, Offset(56f, 72f), Size(18f, 19f), style = line)
            // sweat drop
            drawCircle(Color(0xFF6EC6FF), 5f, Offset(118f, 40f))
            drawPath(Path().apply { moveTo(118f, 28f); lineTo(113.5f, 38f); lineTo(122.5f, 38f); close() }, Color(0xFF6EC6FF))
        }
    }
    // wheels
    for (x in listOf(18f, 86f)) {
        drawCircle(Ink, 13f, Offset(x + 13f, 91f))
        drawCircle(Color(0xFF9AA3BD), 11f, Offset(x + 13f, 91f), style = Stroke(4f))
    }
}

private fun DrawScope.ear(x: Float, deg: Float) {
    rotate(deg, Offset(x + 10f, 38f)) {
        drawRoundRect(MintDark, Offset(x, -6f), Size(20f, 44f), CornerRadius(10f, 22f))
    }
}
