package com.pleosplayground.drivequest.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface VehicleSignalSource {
    val ux: StateFlow<UxState>
    fun trip(): Flow<Sample>
}
