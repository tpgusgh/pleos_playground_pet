package com.pleosplayground.drivequest.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pleosplayground.drivequest.core.FakeSignalSource
import com.pleosplayground.drivequest.core.Scenario
import com.pleosplayground.drivequest.data.HeroStore

class MainActivity : ComponentActivity() {
    private val vm: GameViewModel by viewModels {
        viewModelFactory {
            initializer {
                GameViewModel(
                    FakeSignalSource(Scenario.SMOOTH, tickDelayMs = 150),
                    HeroStore(applicationContext),
                    Speaker(applicationContext),
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface { GameScreen(vm) }
            }
        }
    }
}
