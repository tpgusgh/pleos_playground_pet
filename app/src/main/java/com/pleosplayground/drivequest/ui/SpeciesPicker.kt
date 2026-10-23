package com.pleosplayground.drivequest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pleosplayground.drivequest.core.Face
import com.pleosplayground.drivequest.core.Species
import com.pleosplayground.drivequest.core.speciesFromId

/**
 * Pick a friend: six cards, a big preview of the chosen one and a confirm button (only once something is chosen).
 * The preview and button slots keep their final size from the start, so choosing a card never moves the grid.
 * Landscape puts the preview and buttons beside the grid so the confirm button stays on screen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpeciesPicker(
    initial: Species?,
    onConfirm: (Species) -> Unit,
    onClose: (() -> Unit)?,
    modifier: Modifier = Modifier,
    landscape: Boolean = false,
) {
    var chosenId by rememberSaveable { mutableStateOf(initial?.id) }
    val chosen = speciesFromId(chosenId)
    val title: @Composable () -> Unit = {
        Text("함께 달릴 친구를 골라 주세요", color = Line, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
    val grid: @Composable (Modifier) -> Unit = { m ->
        FlowRow(
            m,
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Species.entries.forEach { s -> SpeciesCard(s, s == chosen) { chosenId = s.id } }
        }
    }
    val preview: @Composable () -> Unit = {
        Column(Modifier.height(188.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (chosen != null) {
                PetFace(Face.HAPPY, chosen, Modifier.height(160.dp))
                Text("${chosen.displayName} · ${chosen.tagline}", color = Line, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            } else {
                Text("카드를 눌러 보세요", color = Line, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
    // The button row keeps its height before anything is chosen, so nothing moves when the confirm button appears.
    val buttons: @Composable () -> Unit = {
        Row(Modifier.heightIn(min = 51.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onClose != null) PillButton("닫기", Yellow, onClose)
            if (chosen != null) PillButton("이 아이로 시작", Pink) { onConfirm(chosen) }
        }
    }
    if (landscape) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            title()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                grid(Modifier.weight(1f))
                Column(Modifier.width(240.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    preview()
                    buttons()
                }
            }
        }
    } else {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            title()
            preview()
            grid(Modifier.fillMaxWidth())
            buttons()
        }
    }
}

@Composable
private fun SpeciesCard(species: Species, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier
            .width(140.dp)
            .background(if (selected) Color(0xFFFFE8A3) else Color(0xFFFFF7E8), shape)
            .border(3.dp, Line, shape)
            .semantics(mergeDescendants = true) { this.selected = selected }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        PetFace(Face.HAPPY, species, Modifier.height(72.dp))
        Text(species.displayName, color = Line, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(species.tagline, color = Line, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}
