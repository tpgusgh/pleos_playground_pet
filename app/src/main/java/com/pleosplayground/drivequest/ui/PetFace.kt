package com.pleosplayground.drivequest.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import com.pleosplayground.drivequest.core.Face
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// ---- timing (pure, unit-tested) -------------------------------------------------------------

/** Length of the animation loop; every periodic motion below divides it so the loop is seamless. */
internal const val LoopMs = 10_000
private val BlinkStarts = longArrayOf(2600, 6200, 6450, 9300) // 6200/6450 is a double blink
private const val BlinkMs = 180
private val TwitchStarts = longArrayOf(1300, 5600)
private const val TwitchMs = 450

/** Eye openness, 1 = open .. 0 = shut, at [ms] on the loop clock. Each blink shuts and reopens linearly. */
internal fun eyeOpenness(ms: Long): Float {
    val t = Math.floorMod(ms, LoopMs.toLong())
    for (s in BlinkStarts) {
        val d = t - s
        if (d in 0 until BlinkMs) return abs(1f - 2f * d / BlinkMs)
    }
    return 1f
}

/** Extra ear angle in degrees at [ms]: a quick damped flick twice per loop, 0 otherwise. */
internal fun earTwitch(ms: Long): Float {
    val t = Math.floorMod(ms, LoopMs.toLong())
    for (s in TwitchStarts) {
        val d = t - s
        if (d in 0 until TwitchMs) {
            val p = d.toFloat() / TwitchMs
            return 7f * sin(p * 3f * PI.toFloat()) * (1f - p)
        }
    }
    return 0f
}

/** 0..1 sine wave with [periodMs] (must divide [LoopMs]) and a phase in radians. */
private fun wave(ms: Long, periodMs: Int, phase: Float = 0f): Float =
    0.5f - 0.5f * cos(2f * PI.toFloat() * (ms % periodMs) / periodMs + phase)

// ---- palette and strokes (design units: the car lives in a 200x200 box, drawn 20 units in) --

private const val DW = 240f
private const val DH = 200f
private val Mint = Color(0xFFBFEEDD)
private val MintShade = Color(0xFF9EDCC5)
private val EarFill = Color(0xFFA8E3CC)
private val Cream = Color(0xFFFFF6E2)
private val GlassTop = Color(0xFFE4F4FF)
private val GlassLow = Color(0xFFB8DDF7)
private val PinkGlass = Color(0xFFFFC9D6)
private val Cheek = Color(0xFFFFB3C6)
private val MouthFill = Color(0xFFE8798F)
private val Tongue = Color(0xFFFFB5C3)
private val Lamp = Color(0xFFFFE08A)
private val Tire = Color(0xFF9A8679)
private val Hub = Color(0xFFFFF6E2)
private val Heart = Color(0xFFFF8FA8)
private val Star = Color(0xFFFFE08A)
private val Water = Color(0xFFBFE6FF)
private val CloudFill = Color(0xFFDCE3EE)
private val White = Color.White
private val Nori = Color(0xFF5E6B63)
private val Rain = Color(0xFF8FCBF2)
private val Outline = Stroke(3f, join = StrokeJoin.Round)
private val Detail = Stroke(2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
private val Fine = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
private val Highlight = Stroke(4f, cap = StrokeCap.Round)
private val TearX = floatArrayOf(64f, 136f)
private val LampX = floatArrayOf(34f, 166f)
private val RainX = floatArrayOf(140f, 161f)
private val RumbleY = floatArrayOf(124f, 136f)
private val GlassBrush = Brush.verticalGradient(listOf(GlassTop, GlassLow), startY = 47f, endY = 86f)

@Composable
fun PetFace(face: Face, modifier: Modifier = Modifier) {
    val desc = when (face) {
        Face.HAPPY -> "행복한 차 캐릭터"
        Face.SULKY -> "시무룩한 차 캐릭터"
        Face.HUNGRY -> "배고픈 차 캐릭터"
    }
    // One linear clock drives breathing, blinking, twitches and effects; values are read in the draw phase only.
    val clock = rememberInfiniteTransition(label = "pet").animateFloat(
        0f, LoopMs.toFloat(), infiniteRepeatable(tween(LoopMs, easing = LinearEasing)), label = "clock",
    )
    val bouncy = spring<Float>(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow)
    val ear = animateFloatAsState(
        when (face) { Face.HAPPY -> 10f; Face.HUNGRY -> 30f; Face.SULKY -> 118f }, bouncy, label = "ear",
    )
    val brow = animateFloatAsState(
        when (face) { Face.HAPPY -> 0f; Face.HUNGRY -> 12f; Face.SULKY -> 22f }, tween(400), label = "brow",
    )
    val blush = animateFloatAsState(
        when (face) { Face.HAPPY -> 0.95f; Face.HUNGRY -> 0.7f; Face.SULKY -> 0.55f }, tween(500), label = "blush",
    )
    val happyFx = animateFloatAsState(if (face == Face.HAPPY) 1f else 0f, tween(500), label = "happyFx")
    val sulkyFx = animateFloatAsState(if (face == Face.SULKY) 1f else 0f, tween(500), label = "sulkyFx")
    val hungryFx = animateFloatAsState(if (face == Face.HUNGRY) 1f else 0f, tween(500), label = "hungryFx")
    val p = remember { CarPaths() }
    // "꼬르륵" for the hungry gag; sized in design units (px inside the scaled canvas), measured once.
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val rumble = remember(measurer, density) {
        measurer.measure("꼬르륵", TextStyle(color = Line, fontSize = with(density) { 12f.toSp() }, fontWeight = FontWeight.Bold))
    }
    Canvas(modifier.aspectRatio(DW / DH).semantics { contentDescription = desc }) {
        scale(size.width / DW, pivot = Offset.Zero) {
            translate(20f, 0f) {
                val ms = clock.value.toLong()
                drawCar(p, face, ms, ear.value, brow.value, blush.value)
                drawEffects(p, rumble, ms, happyFx.value, sulkyFx.value, hungryFx.value)
            }
        }
    }
}

// ---- paths, built once per PetFace ------------------------------------------------------------

private class CarPaths {
    val body = Path().apply {
        val cabin = Path().apply {
            addRoundRect(RoundRect(46f, 36f, 154f, 104f, CornerRadius(40f, 38f), CornerRadius(40f, 38f), CornerRadius.Zero, CornerRadius.Zero))
        }
        val chassis = Path().apply {
            addRoundRect(RoundRect(16f, 80f, 184f, 170f, CornerRadius(36f, 30f), CornerRadius(36f, 30f), CornerRadius(28f, 24f), CornerRadius(28f, 24f)))
        }
        op(cabin, chassis, PathOperation.Union)
    }
    val windshield = Path().apply {
        moveTo(76f, 47f); lineTo(124f, 47f); quadraticTo(132f, 47f, 134f, 54f)
        lineTo(140f, 77f); quadraticTo(142f, 85f, 134f, 85f)
        lineTo(66f, 85f); quadraticTo(58f, 85f, 60f, 77f)
        lineTo(66f, 54f); quadraticTo(68f, 47f, 76f, 47f); close()
    }
    val happyMouth = Path().apply {
        moveTo(91f, 128f); lineTo(109f, 128f)
        cubicTo(109f, 138f, 104f, 142f, 100f, 142f); cubicTo(96f, 142f, 91f, 138f, 91f, 128f); close()
    }
    val hungryMouth = Path().apply { addOval(Rect(91f, 124f, 109f, 145f)) }
    val pout = Path().apply { moveTo(93f, 134f); quadraticTo(100f, 130.5f, 107f, 134f) }
    val plateHeart = heart(100f, 157f, 8f)
    val charm = heart(0f, 0f, 12f)
    val tear = drop(0f, 0f, 11f)
    val sweat = drop(0f, 0f, 15f)
    val drool = drop(0f, 0f, 10f)
    val rain = drop(0f, 0f, 8f)
    val starAt = listOf(Offset(-4f, 46f), Offset(196f, 26f), Offset(205f, 80f))
    val stars = listOf(sparkle(starAt[0], 9f), sparkle(starAt[1], 11f), sparkle(starAt[2], 6f))
    val cloud = Path().apply {
        val a = Path().apply { addOval(Rect(126f, 6f, 146f, 26f)) }
        val b = Path().apply { addOval(Rect(138f, -2f, 164f, 24f)) }
        val c = Path().apply { addOval(Rect(156f, 6f, 176f, 26f)) }
        val base = Path().apply { addRoundRect(RoundRect(126f, 14f, 176f, 28f, CornerRadius(7f))) }
        val ab = Path().apply { op(a, b, PathOperation.Union) }
        val abc = Path().apply { op(ab, c, PathOperation.Union) }
        op(abc, base, PathOperation.Union)
    }
    // thought bubble with a rice ball, upper left of the head
    val onigiri = Path().apply {
        moveTo(2f, 17f)
        cubicTo(5f, 17f, 13f, 30f, 13f, 33f); cubicTo(13f, 36f, 11f, 37f, 8f, 37f)
        lineTo(-4f, 37f); cubicTo(-7f, 37f, -9f, 36f, -9f, 33f); cubicTo(-9f, 30f, -1f, 17f, 2f, 17f); close()
    }
    val squiggle = Path().apply {
        moveTo(0f, 0f); quadraticTo(3f, -3.5f, 6f, 0f); quadraticTo(9f, 3.5f, 12f, 0f)
    }
}

private fun heart(cx: Float, cy: Float, w: Float) = Path().apply {
    val h = w * 0.9f
    moveTo(cx, cy + h * 0.5f)
    cubicTo(cx - w * 0.15f, cy + h * 0.3f, cx - w * 0.55f, cy + h * 0.05f, cx - w * 0.5f, cy - h * 0.2f)
    cubicTo(cx - w * 0.45f, cy - h * 0.55f, cx - w * 0.05f, cy - h * 0.6f, cx, cy - h * 0.25f)
    cubicTo(cx + w * 0.05f, cy - h * 0.6f, cx + w * 0.45f, cy - h * 0.55f, cx + w * 0.5f, cy - h * 0.2f)
    cubicTo(cx + w * 0.55f, cy + h * 0.05f, cx + w * 0.15f, cy + h * 0.3f, cx, cy + h * 0.5f)
    close()
}

/** Tear drop with its point at (cx, top) and height [h]. */
private fun drop(cx: Float, top: Float, h: Float) = Path().apply {
    val w = h * 0.66f
    moveTo(cx, top)
    cubicTo(cx + w * 0.25f, top + h * 0.3f, cx + w / 2, top + h * 0.45f, cx + w / 2, top + h - w / 2)
    arcTo(Rect(cx - w / 2, top + h - w, cx + w / 2, top + h), 0f, 180f, false)
    cubicTo(cx - w / 2, top + h * 0.45f, cx - w * 0.25f, top + h * 0.3f, cx, top)
    close()
}

/** Four-point twinkle centred on (cx, cy). */
private fun sparkle(c: Offset, r: Float) = Path().apply {
    val cx = c.x; val cy = c.y
    val k = r * 0.18f
    moveTo(cx, cy - r)
    quadraticTo(cx + k, cy - k, cx + r, cy); quadraticTo(cx + k, cy + k, cx, cy + r)
    quadraticTo(cx - k, cy + k, cx - r, cy); quadraticTo(cx - k, cy - k, cx, cy - r); close()
}

// ---- drawing ------------------------------------------------------------------------------------

private fun DrawScope.drawCar(p: CarPaths, face: Face, ms: Long, ear: Float, brow: Float, blush: Float) {
    val breath = wave(ms, 2500)
    // ground shadow on the rug: shrinks a touch as the body rises
    val sw = 140f * (1f - 0.05f * breath)
    drawOval(Line.copy(alpha = 0.2f), Offset(100f - sw / 2, 179f), Size(sw, 14f))

    // the body breathes on its wheels: a little rise and stretch, pivoting on the axle line
    withTransform({
        translate(0f, -3f * breath)
        scale(1f - 0.008f * breath, 1f + 0.012f * breath, Offset(100f, 170f))
    }) {
        val sway = 2.2f * (wave(ms, 2000) - 0.5f)
        mirrorEar(60f, -(ear + sway))
        mirrorEar(140f, ear + sway + earTwitch(ms))
        antenna(p, 3f * (wave(ms, 2000, 1f) - 0.5f) + earTwitch(ms + 700) * 0.6f)
        bodyShell(p)
        windshield(p)
        face(p, face, ms, brow, blush, breath)
        frontDetails(p)
    }
    wheel(44f, 170f, 4f * (breath - 0.5f))
    wheel(156f, 170f, 4f * (breath - 0.5f))
}

private fun DrawScope.mirrorEar(px: Float, deg: Float) = rotate(deg, Offset(px, 58f)) {
    drawOval(EarFill, Offset(px - 14f, 4f), Size(28f, 56f))
    drawOval(PinkGlass, Offset(px - 8f, 11f), Size(16f, 36f))
    drawLine(White.copy(alpha = 0.85f), Offset(px - 3f, 17f), Offset(px - 3f, 25f), 2.6f, StrokeCap.Round)
    drawOval(Line, Offset(px - 8f, 11f), Size(16f, 36f), style = Fine)
    drawOval(Line, Offset(px - 14f, 4f), Size(28f, 56f), style = Outline)
}

private fun DrawScope.antenna(p: CarPaths, deg: Float) = rotate(deg, Offset(100f, 40f)) {
    drawLine(Line, Offset(100f, 40f), Offset(100f, 20f), 2.6f, StrokeCap.Round)
    translate(100f, 14f) {
        drawPath(p.charm, Heart)
        drawPath(p.charm, Line, style = Fine)
    }
}

private fun DrawScope.bodyShell(p: CarPaths) {
    drawPath(p.body, Mint)
    clipPath(p.body) {
        drawRect(MintShade.copy(alpha = 0.7f), Offset(0f, 152f), Size(200f, 24f))
        drawOval(MintShade.copy(alpha = 0.35f), Offset(166f, 70f), Size(44f, 110f))
        // light from the top left: a soft arc on the roof and one on the shoulder
        drawArc(White.copy(alpha = 0.7f), 200f, 50f, false, Offset(52f, 42f), Size(60f, 60f), style = Highlight)
        drawArc(White.copy(alpha = 0.6f), 195f, 40f, false, Offset(22f, 86f), Size(60f, 60f), style = Highlight)
    }
    drawPath(p.body, Line, style = Outline)
}

private fun DrawScope.windshield(p: CarPaths) {
    drawPath(p.windshield, GlassBrush)
    clipPath(p.windshield) {
        drawLine(White.copy(alpha = 0.85f), Offset(76f, 90f), Offset(98f, 42f), 7f, StrokeCap.Round)
        drawLine(White.copy(alpha = 0.6f), Offset(94f, 90f), Offset(112f, 50f), 3.5f, StrokeCap.Round)
    }
    drawPath(p.windshield, Line, style = Detail)
}

private fun DrawScope.face(p: CarPaths, face: Face, ms: Long, brow: Float, blush: Float, breath: Float) {
    val eyeY = if (face == Face.SULKY) 115f else 112f
    // cream muzzle patch with a tiny bunny nose
    drawOval(Cream, Offset(76f, 117f), Size(48f, 32f))
    drawOval(Line, Offset(76f, 117f), Size(48f, 32f), style = Fine)
    drawOval(Line, Offset(97.5f, 120.5f), Size(5f, 3.6f))
    // blush
    drawOval(Cheek, Offset(43f, 121f), Size(22f, 11f), alpha = blush)
    drawOval(Cheek, Offset(135f, 121f), Size(22f, 11f), alpha = blush)
    // brows: small arches, tilted inner-end-up when worried
    browArc(72f, eyeY - 13f, -brow)
    browArc(128f, eyeY - 13f, brow)
    val open = eyeOpenness(ms)
    eye(72f, eyeY, open, face)
    eye(128f, eyeY, open, face)
    if (face == Face.SULKY) {
        val s = 0.85f + 0.2f * breath
        for (x in TearX) {
            scale(s, Offset(x, eyeY + 6f)) {
                translate(x, eyeY + 4f) {
                    drawPath(p.tear, Water)
                    drawPath(p.tear, Line, style = Fine)
                }
            }
        }
    }
    when (face) {
        Face.HAPPY -> openMouth(p.happyMouth, 93f, 134f)
        Face.HUNGRY -> {
            openMouth(p.hungryMouth, 93f, 136f)
            scale(1f, 0.8f + 0.35f * wave(ms, 2500, 1.5f), Offset(108.5f, 136f)) {
                translate(108.5f, 136f) {
                    drawPath(p.drool, Water)
                    drawPath(p.drool, Line, style = Fine)
                }
            }
        }
        Face.SULKY -> drawPath(p.pout, Line, style = Detail)
    }
}

private fun DrawScope.browArc(cx: Float, cy: Float, deg: Float) = rotate(deg, Offset(cx, cy)) {
    drawArc(Line, 205f, 130f, false, Offset(cx - 8f, cy - 2f), Size(16f, 8f), style = Detail)
}

private fun DrawScope.eye(cx: Float, cy: Float, open: Float, face: Face) {
    if (open < 0.3f) { // shut: a soft downward curve
        drawArc(Line, 20f, 140f, false, Offset(cx - 6.5f, cy - 4f), Size(13f, 7f), style = Detail)
        return
    }
    val h = 14.5f * open
    drawOval(Line, Offset(cx - 6f, cy - h / 2), Size(12f, h))
    if (open > 0.7f) {
        drawCircle(White, 2.9f, Offset(cx - 2f, cy - 3.2f))
        if (face == Face.HAPPY) drawCircle(White, 1.4f, Offset(cx + 2.5f, cy + 3f))
        if (face == Face.SULKY) drawArc(Water, 20f, 140f, false, Offset(cx - 5f, cy - 2.5f), Size(10f, 9f), style = Fine)
    }
}

private fun DrawScope.openMouth(path: Path, tongueX: Float, tongueY: Float) {
    drawPath(path, MouthFill)
    clipPath(path) { drawOval(Tongue, Offset(tongueX, tongueY), Size(14f, 10f)) }
    drawPath(path, Line, style = Detail)
}

private fun DrawScope.frontDetails(p: CarPaths) {
    // headlights
    for (x in LampX) {
        drawCircle(Lamp, 8f, Offset(x, 138f))
        drawCircle(Line, 8f, Offset(x, 138f), style = Detail)
        drawCircle(White, 2.4f, Offset(x - 2.5f, 135.5f))
    }
    // bumper and a little heart plate
    drawRoundRect(Cream, Offset(58f, 154f), Size(84f, 12f), CornerRadius(6f))
    drawRoundRect(Line, Offset(58f, 154f), Size(84f, 12f), CornerRadius(6f), style = Detail)
    drawRoundRect(White, Offset(87f, 149f), Size(26f, 16f), CornerRadius(4f))
    drawRoundRect(Line, Offset(87f, 149f), Size(26f, 16f), CornerRadius(4f), style = Fine)
    drawPath(p.plateHeart, Heart)
}

private fun DrawScope.wheel(cx: Float, cy: Float, deg: Float) {
    val c = Offset(cx, cy)
    drawCircle(Tire, 16f, c)
    drawCircle(Line, 16f, c, style = Outline)
    drawCircle(Hub, 9.5f, c)
    drawCircle(Line, 9.5f, c, style = Fine)
    for (i in 0 until 5) {
        val a = (deg + i * 72f - 90f) * PI.toFloat() / 180f
        drawLine(Line.copy(alpha = 0.7f), c, Offset(cx + 7f * cos(a), cy + 7f * sin(a)), 1.8f, StrokeCap.Round)
    }
    drawCircle(Heart, 3f, c)
    drawCircle(Line, 3f, c, style = Fine)
}

private fun DrawScope.drawEffects(p: CarPaths, rumble: TextLayoutResult, ms: Long, happy: Float, sulky: Float, hungry: Float) {
    if (happy > 0f) {
        for (i in p.stars.indices) {
            val star = p.stars[i]
            val s = 0.7f + 0.4f * wave(ms, 1250, i * 2.1f)
            scale(s * happy, p.starAt[i]) {
                drawPath(star, Star)
                drawPath(star, Line, style = Fine)
            }
        }
    }
    if (sulky > 0f) {
        val bob = 2f * wave(ms, 2500)
        translate(0f, bob) {
            drawPath(p.cloud, CloudFill, alpha = sulky)
            drawPath(p.cloud, Line, alpha = sulky, style = Detail)
            for (i in RainX.indices) {
                val x = RainX[i]
                val f = ((ms + i * 500) % 1000) / 1000f
                translate(x, 31f + 9f * f) {
                    drawPath(p.rain, Rain, alpha = sulky * (1f - 0.6f * f))
                    drawPath(p.rain, Line, alpha = sulky * (1f - 0.6f * f), style = Fine)
                }
            }
        }
    }
    if (hungry > 0f) {
        val float = 2f * wave(ms, 2000)
        translate(0f, -float) {
            drawCircle(White, 18f, Offset(2f, 28f), alpha = hungry)
            drawCircle(Line, 18f, Offset(2f, 28f), alpha = hungry, style = Detail)
            drawPath(p.onigiri, White, alpha = hungry)
            drawPath(p.onigiri, Line, alpha = hungry, style = Detail)
            drawRoundRect(Nori, Offset(-3f, 29f), Size(10f, 8f), CornerRadius(1.5f), alpha = hungry)
        }
        drawCircle(White, 4f, Offset(26f, 51f - float / 2), alpha = hungry)
        drawCircle(Line, 4f, Offset(26f, 51f - float / 2), alpha = hungry, style = Fine)
        drawCircle(White, 2.6f, Offset(35f, 61f), alpha = hungry)
        drawCircle(Line, 2.6f, Offset(35f, 61f), alpha = hungry, style = Fine)
        // sweat drop beside the cabin
        translate(166f, 54f + 2f * wave(ms, 2500)) {
            drawPath(p.sweat, Water, alpha = hungry)
            drawPath(p.sweat, Line, alpha = hungry, style = Fine)
        }
        // tummy rumble: squiggles on both sides that come in bursts
        val a = hungry * (0.5f + 0.5f * wave(ms, 2500))
        val jx = 1.2f * (wave(ms, 250) - 0.5f)
        for (y in RumbleY) translate(-2f + jx, y) { drawPath(p.squiggle, Line, alpha = a, style = Detail) }
        translate(190f - jx, 120f) { drawPath(p.squiggle, Line, alpha = a, style = Detail) }
        rotate(-6f, Offset(200f, 136f)) { drawText(rumble, topLeft = Offset(186f + jx, 128f), alpha = a) }
    }
}
