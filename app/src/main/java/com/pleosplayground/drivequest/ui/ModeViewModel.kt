package com.pleosplayground.drivequest.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import com.pleosplayground.drivequest.core.AppMode
import com.pleosplayground.drivequest.data.ModeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

class ModeViewModel(private val store: ModeRepository) : ViewModel() {
    private val _mode = MutableStateFlow<AppMode?>(null)
    val mode: StateFlow<AppMode?> = _mode.asStateFlow()
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    // One PetViewModel per mode (key = mode.id), cleared on mode change so the old narrator shuts down.
    val petViewModels = ViewModelStore()

    // A failed read shows the start screen. choose/clear join this so the read cannot overwrite a choice.
    private val read = viewModelScope.launch {
        _mode.value = try {
            store.loadMode()
        } catch (e: IOException) {
            null
        }
        _loaded.value = true
    }

    fun choose(mode: AppMode) {
        viewModelScope.launch {
            read.join()
            if (_mode.value != mode) petViewModels.clear()
            _mode.value = mode
            // ponytail: mode save failure is ignored, the next launch may show the start screen again
            try { store.saveMode(mode) } catch (e: IOException) {}
        }
    }

    fun clear() {
        viewModelScope.launch {
            read.join()
            petViewModels.clear()
            _mode.value = null
            // ponytail: mode clear failure is ignored, the next launch may restore the previous mode
            try { store.clearMode() } catch (e: IOException) {}
        }
    }

    override fun onCleared() {
        petViewModels.clear()
    }
}
