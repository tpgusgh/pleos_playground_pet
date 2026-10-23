package com.pleosplayground.drivequest.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// ponytail: replace with the Pleos Vehicle SDK implementation; ux must come from CarUxRestrictions
class UnavailableSignalSource : VehicleSignalSource {
    override val ux: StateFlow<UxState> = MutableStateFlow(UxState.RESTRICTED)
    override val state: StateFlow<Sample> = MutableStateFlow(Sample())
    override val connection: StateFlow<ConnectionState> = MutableStateFlow(ConnectionState.UNAVAILABLE)
}
