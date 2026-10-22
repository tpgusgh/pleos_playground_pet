package com.pleosplayground.drivequest.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.pleosplayground.drivequest.core.Face

private val Mint = Color(0xFFBFEEDD)
private val EarFill = Color(0xFFA8E3CC)
private val Cheek = Color(0xFFFFC9D6)
private const val W = 3f // outline width in design units

// Drawn in 150x146 design units (130x96 body, ears above, wheels below), scaled to fit the canvas.
@Composable
fun PetFace(face: Face, modifier: Modifier = Modifier) {
    val desc = when (face) {
        Face.HAPPY -> "행복한 차 캐릭터"
        Face.SULKY -> "시무룩한 차 캐릭터"
        Face.HUNGRY -> "배고픈 차 캐릭터"
    }
    Canvas(modifier.aspectRatio(150f / 146f).semantics { contentDescription = desc }) {
        scale(size.width / 150f, pivot = Offset.Zero) { drawCar(face) }
    }
}

private fun DrawScope.drawCar(face: Face) {
    val sulky = face == Face.SULKY
    val outline = Stroke(W)
    val round = Stroke(W, cap = StrokeCap.Round)
    // ears stand up; they flop sideways when sulky
    val tilt = if (sulky) 62f else 22f
    ear(30f, -tilt)
    ear(94f, tilt)
    val body = Path().apply {
        addRoundRect(
            RoundRect(
                10f, 32f, 140f, 128f,
                CornerRadius(62f, 46f), CornerRadius(62f, 46f), CornerRadius(49f, 36f), CornerRadius(49f, 36f),
            ),
        )
    }
    drawPath(body, Mint)
    drawPath(body, Line, style = outline)
    // eyes: small dots, set a little lower when sulky
    val ey = if (sulky) 74f else 68f
    drawOval(Line, Offset(48f, ey), Size(9f, 11f))
    drawOval(Line, Offset(93f, ey), Size(9f, 11f))
    if (sulky) {
        drawLine(Line, Offset(43f, 66f), Offset(59f, 61f), W, StrokeCap.Round)
        drawLine(Line, Offset(107f, 66f), Offset(91f, 61f), W, StrokeCap.Round)
    }
    // blush
    drawOval(Cheek.copy(alpha = 0.8f), Offset(32f, 86f), Size(16f, 9f))
    drawOval(Cheek.copy(alpha = 0.8f), Offset(102f, 86f), Size(16f, 9f))
    when (face) {
        Face.HAPPY -> drawArc(Line, 0f, 180f, false, Offset(65f, 82f), Size(20f, 14f), style = round)
        Face.SULKY -> drawArc(Line, 180f, 180f, false, Offset(67f, 94f), Size(16f, 9f), style = round)
        Face.HUNGRY -> {
            drawOval(Color(0xFFFF9FB0), Offset(66f, 86f), Size(18f, 16f))
            drawOval(Line, Offset(66f, 86f), Size(18f, 16f), style = outline)
            // sweat drop
            val drop = Path().apply {
                moveTo(128f, 44f); cubicTo(132f, 51f, 135f, 54f, 135f, 58f)
                cubicTo(135f, 62f, 131f, 65f, 128f, 65f); cubicTo(125f, 65f, 121f, 62f, 121f, 58f)
                cubicTo(121f, 54f, 124f, 51f, 128f, 44f); close()
            }
            drawPath(drop, Color(0xFFBFE6FF))
            drawPath(drop, Line, style = Stroke(2f))
        }
    }
    // wheels: white with the same outline
    for (cx in listOf(42f, 108f)) {
        drawCircle(Color.White, 12f, Offset(cx, 128f))
        drawCircle(Line, 12f, Offset(cx, 128f), style = outline)
    }
}

private fun DrawScope.ear(x: Float, deg: Float) {
    rotate(deg, Offset(x + 13f, 58f)) {
        drawOval(EarFill, Offset(x, 12f), Size(26f, 44f))
        drawOval(Line, Offset(x, 12f), Size(26f, 44f), style = Stroke(W))
    }
}
