package com.pleosplayground.drivequest.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelProvider
import com.pleosplayground.drivequest.core.AppMode
import com.pleosplayground.drivequest.core.UnavailableSignalSource
import com.pleosplayground.drivequest.core.petKeyFor
import com.pleosplayground.drivequest.core.showDevTools
import com.pleosplayground.drivequest.data.ModeStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pleosplayground.drivequest.core.FakeSignalSource
import com.pleosplayground.drivequest.data.PetStore

class MainActivity : ComponentActivity() {
    private val modeVm: ModeViewModel by viewModels {
        viewModelFactory { initializer { ModeViewModel(ModeStore(applicationContext)) } }
    }

    private fun petFactory(mode: AppMode) = viewModelFactory {
        initializer {
            when (mode) {
                AppMode.DEV -> FakeSignalSource(tickDelayMs = 150).let {
                    PetViewModel(it, it, PetStore(applicationContext, petKeyFor(mode)), Speaker(applicationContext))
                }
                AppMode.CONNECTED ->
                    PetViewModel(UnavailableSignalSource(), null, PetStore(applicationContext, petKeyFor(mode)), Speaker(applicationContext))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT))
        setContent {
            MaterialTheme(colorScheme = androidx.compose.material3.lightColorScheme(
                primary = androidx.compose.ui.graphics.Color(0xFF6B5648),
                secondaryContainer = androidx.compose.ui.graphics.Color(0xFFFFC9D6),
                onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF6B5648),
                surfaceVariant = androidx.compose.ui.graphics.Color(0xFFF1CF9F),
                outline = androidx.compose.ui.graphics.Color(0xFF6B5648),
            )) {
                Surface(color = androidx.compose.ui.graphics.Color(0xFFFFF1D6)) {
                    val loaded by modeVm.loaded.collectAsState()
                    val mode by modeVm.mode.collectAsState()
                    val active = mode
                    when {
                        !loaded -> Unit
                        active == null -> StartScreen(modeVm::choose)
                        else -> {
                            val vm = remember(active) {
                                ViewModelProvider(modeVm.petViewModels, petFactory(active))[active.id, PetViewModel::class.java]
                            }
                            PetScreen(
                                vm,
                                devTools = showDevTools(active),
                                onSwitchMode = if (active == AppMode.DEV) modeVm::clear else ({ modeVm.choose(AppMode.DEV) }),
                            )
                        }
                    }
                }
            }
        }
    }
}
