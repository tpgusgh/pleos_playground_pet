package com.pleosplayground.drivequest.core

import kotlinx.coroutines.flow.StateFlow

enum class ConnectionState { CONNECTING, CONNECTED, UNAVAILABLE }

fun connectionLabel(state: ConnectionState): String? = when (state) {
    ConnectionState.CONNECTING -> "차량과 연결하는 중이에요"
    ConnectionState.UNAVAILABLE -> "차량과 연결되지 않았어요"
    ConnectionState.CONNECTED -> null
}

interface VehicleSignalSource {
    val ux: StateFlow<UxState>
    val state: StateFlow<Sample>
    val connection: StateFlow<ConnectionState>
}
