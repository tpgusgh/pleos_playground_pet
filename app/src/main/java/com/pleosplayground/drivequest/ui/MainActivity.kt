package com.pleosplayground.drivequest.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pleosplayground.drivequest.core.FakeSignalSource
import com.pleosplayground.drivequest.data.PetStore

class MainActivity : ComponentActivity() {
    private val vm: PetViewModel by viewModels {
        viewModelFactory {
            initializer {
                PetViewModel(
                    FakeSignalSource(tickDelayMs = 150),
                    PetStore(applicationContext),
                    Speaker(applicationContext),
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        setContent {
            MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme()) {
                Surface(color = androidx.compose.ui.graphics.Color(0xFF1D2333)) { PetScreen(vm) }
            }
        }
    }
}
