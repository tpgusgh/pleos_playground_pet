package com.pleosplayground.drivequest.core

enum class AppMode(val id: String) { DEV("dev"), CONNECTED("connected") }

fun modeFromId(id: String?): AppMode? = AppMode.entries.firstOrNull { it.id == id }

fun petKeyFor(mode: AppMode): String = when (mode) {
    AppMode.DEV -> "pet_dev"
    AppMode.CONNECTED -> "pet"
}

fun showDevTools(mode: AppMode): Boolean = mode == AppMode.DEV
