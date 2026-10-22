package com.pleosplayground.drivequest.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pleosplayground.drivequest.core.Scenario
import com.pleosplayground.drivequest.core.UxState
import com.pleosplayground.drivequest.core.canPlay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameScreen(vm: GameViewModel) {
    val hero by vm.hero.collectAsState()
    val score by vm.lastScore.collectAsState()
    val battle by vm.lastBattle.collectAsState()
    val running by vm.running.collectAsState()
    val scenario by vm.scenario.collectAsState()
    val ux by vm.ux.collectAsState()
    val playable = canPlay(ux)

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Drive Quest", style = MaterialTheme.typography.headlineMedium)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Lv ${hero.level}   경험치 ${hero.xp}/${100 * hero.level}")
                Text("HP ${hero.hp}  공격 ${hero.atk}  방어 ${hero.def}  민첩 ${hero.agi}")
            }
        }

        // 개발용 가짜 신호 컨트롤
        Text("개발용 가짜 신호", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Scenario.entries.forEach {
                FilterChip(selected = it == scenario, onClick = { vm.setScenario(it) }, label = { Text(it.name) })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Switch(checked = ux == UxState.PARKED, onCheckedChange = vm::setParked)
            Text(if (playable) "정차 중" else "주행 중")
        }
        Button(onClick = vm::runTrip, enabled = !running) { Text(if (running) "주행 중..." else "주행 시작") }

        score?.let { s ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("지난 주행", style = MaterialTheme.typography.titleSmall)
                    Text("경험치 ${s.xp}  골드 ${s.gold}  거리 ${"%.1f".format(s.km)}km")
                    Text("급제동 ${s.hardBrakes}  부드러운 제동 ${s.smoothBrakes}  급조향 ${s.sharpSteers}  콤보 ${s.bestCombo}")
                    if (s.flags.isNotEmpty()) Text(s.flags.joinToString(" "))
                }
            }
        }

        if (!playable) Text("정차 후 이용할 수 있어요", color = MaterialTheme.colorScheme.error)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = vm::battle, enabled = playable) { Text("전투") }
            Button(onClick = vm::bossBattle, enabled = playable) { Text("보스전") }
        }
        battle?.let { b ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(if (b.won) "승리 (${b.rounds}라운드)" else "패배 (${b.rounds}라운드)")
                    b.log.takeLast(8).forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}
