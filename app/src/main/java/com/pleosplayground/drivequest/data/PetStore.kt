package com.pleosplayground.drivequest.data

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pleosplayground.drivequest.core.AppMode
import com.pleosplayground.drivequest.core.PetState
import com.pleosplayground.drivequest.core.Species
import com.pleosplayground.drivequest.core.decodePet
import com.pleosplayground.drivequest.core.encode
import com.pleosplayground.drivequest.core.modeFromId
import com.pleosplayground.drivequest.core.speciesFromId
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(
    name = "drivequest",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)
private val MODE_KEY = stringPreferencesKey("mode")
private val SPECIES_KEY = stringPreferencesKey("species")

interface PetRepository {
    suspend fun load(): PetState
    suspend fun save(state: PetState)
    suspend fun loadSpecies(): Species? // null: not chosen yet, or an unknown stored value
    suspend fun saveSpecies(species: Species)
}

class PetStore(context: Context, petKey: String = "pet") : PetRepository {
    private val store = context.applicationContext.dataStore
    private val PET_KEY = stringPreferencesKey(petKey)

    override suspend fun load(): PetState = decodePet(store.data.first()[PET_KEY])

    override suspend fun save(state: PetState) {
        store.edit { it[PET_KEY] = state.encode() }
    }

    override suspend fun loadSpecies(): Species? = speciesFromId(store.data.first()[SPECIES_KEY])

    override suspend fun saveSpecies(species: Species) {
        store.edit { it[SPECIES_KEY] = species.id }
    }
}

interface ModeRepository {
    suspend fun loadMode(): AppMode? // null: not chosen yet, or an unknown stored value
    suspend fun saveMode(mode: AppMode)
    suspend fun clearMode()
}

// Shares the one Context.dataStore above: a second preferencesDataStore on the same file throws at runtime.
class ModeStore(context: Context) : ModeRepository {
    private val store = context.applicationContext.dataStore

    override suspend fun loadMode(): AppMode? = modeFromId(store.data.first()[MODE_KEY])

    override suspend fun saveMode(mode: AppMode) {
        store.edit { it[MODE_KEY] = mode.id }
    }

    override suspend fun clearMode() {
        store.edit { it.remove(MODE_KEY) }
    }
}
