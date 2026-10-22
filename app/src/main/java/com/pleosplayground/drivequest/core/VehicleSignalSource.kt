package com.pleosplayground.drivequest.core

import kotlinx.coroutines.flow.StateFlow

interface VehicleSignalSource {
    val ux: StateFlow<UxState>
    val state: StateFlow<Sample>
}
