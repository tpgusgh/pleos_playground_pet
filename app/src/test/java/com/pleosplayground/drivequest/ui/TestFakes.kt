package com.pleosplayground.drivequest.ui

import com.pleosplayground.drivequest.core.AppMode
import com.pleosplayground.drivequest.core.PetState
import com.pleosplayground.drivequest.core.Species
import com.pleosplayground.drivequest.core.modeFromId
import com.pleosplayground.drivequest.data.ModeRepository
import com.pleosplayground.drivequest.data.PetRepository
import kotlinx.coroutines.CompletableDeferred
import java.io.IOException

internal class FakeRepo(
    private val initial: PetState = PetState(),
    private val failSave: Boolean = false,
    private val failLoad: Boolean = false,
    private val gate: CompletableDeferred<Unit>? = null,
    private val initialSpecies: Species? = null,
    private val saveGate: CompletableDeferred<Unit>? = null,
) : PetRepository {
    val saved = mutableListOf<PetState>()
    val savedSpecies = mutableListOf<Species>()

    override suspend fun load(): PetState {
        gate?.await()
        if (failLoad) throw IOException("load")
        return initial
    }

    override suspend fun save(state: PetState) {
        saved += state
        if (failSave) throw IOException("save")
    }

    override suspend fun loadSpecies(): Species? {
        gate?.await()
        if (failLoad) throw IOException("load")
        return initialSpecies
    }

    override suspend fun saveSpecies(species: Species) {
        savedSpecies += species
        saveGate?.await()
        if (failSave) throw IOException("save")
    }
}

internal class FakeNarrator : Narrator {
    val lines = mutableListOf<String>()
    override fun speak(text: String) { lines += text }
    override fun shutdown() {}
}

internal class FakeModeStore(
    var stored: String? = null,
    private val failLoad: Boolean = false,
    private val failSave: Boolean = false,
    private val gate: CompletableDeferred<Unit>? = null,
) : ModeRepository {
    override suspend fun loadMode(): AppMode? {
        gate?.await()
        if (failLoad) throw IOException("load")
        return modeFromId(stored)
    }

    override suspend fun saveMode(mode: AppMode) {
        if (failSave) throw IOException("save")
        stored = mode.id
    }

    override suspend fun clearMode() {
        if (failSave) throw IOException("save")
        stored = null
    }
}
