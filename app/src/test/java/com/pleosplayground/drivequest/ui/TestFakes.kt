package com.pleosplayground.drivequest.ui

import com.pleosplayground.drivequest.core.PetState
import com.pleosplayground.drivequest.core.Species
import com.pleosplayground.drivequest.data.PetRepository
import kotlinx.coroutines.CompletableDeferred
import java.io.IOException

internal class FakeRepo(
    private val initial: PetState = PetState(),
    private val failSave: Boolean = false,
    private val failLoad: Boolean = false,
    private val gate: CompletableDeferred<Unit>? = null,
    private val initialSpecies: Species? = null,
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
        if (failSave) throw IOException("save")
    }
}

internal class FakeNarrator : Narrator {
    val lines = mutableListOf<String>()
    override fun speak(text: String) { lines += text }
    override fun shutdown() {}
}
