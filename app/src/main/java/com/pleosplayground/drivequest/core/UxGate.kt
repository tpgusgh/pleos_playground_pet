package com.pleosplayground.drivequest.core

enum class UxState { PARKED, RESTRICTED }

fun canPlay(ux: UxState): Boolean = ux == UxState.PARKED
