package com.pleosplayground.drivequest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pleosplayground.drivequest.core.AppMode
import com.pleosplayground.drivequest.core.Face
import com.pleosplayground.drivequest.core.Species

@Composable
fun StartScreen(onPick: (AppMode) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        val landscape = maxWidth > maxHeight
        // Content smaller than the screen is centered; larger content scrolls.
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PetFace(Face.HAPPY, Species.RABBIT, Modifier.height(160.dp).width(120.dp))
            Text("무엇으로 시작할까요?", color = Line, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (landscape) Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModeCard("개발 모드", "가짜 신호로 시험해요", Yellow, Modifier.width(260.dp)) { onPick(AppMode.DEV) }
                ModeCard("차량 연결", "차량 신호로 키워요 (준비 중)", Pink, Modifier.width(260.dp)) { onPick(AppMode.CONNECTED) }
            } else Column(Modifier.fillMaxWidth(), Arrangement.spacedBy(16.dp), Alignment.CenterHorizontally) {
                val w = Modifier.fillMaxWidth().widthIn(max = 360.dp)
                ModeCard("개발 모드", "가짜 신호로 시험해요", Yellow, w) { onPick(AppMode.DEV) }
                ModeCard("차량 연결", "차량 신호로 키워요 (준비 중)", Pink, w) { onPick(AppMode.CONNECTED) }
            }
        }
    }
}

@Composable
private fun ModeCard(title: String, subtitle: String, fill: Color, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier
            .drawBehind { // same hard 3dp bottom shadow as PillButton
                drawRoundRect(Line, Offset(0f, 3.dp.toPx()), size, CornerRadius(22.dp.toPx()))
            }
            .background(fill, shape)
            .border(3.dp, Line, shape)
            .clip(shape)
            .semantics(mergeDescendants = true) {}
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 88.dp)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, color = Line, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Line, fontSize = 14.sp)
    }
}
