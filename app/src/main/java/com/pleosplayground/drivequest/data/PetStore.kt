package com.pleosplayground.drivequest.data

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pleosplayground.drivequest.core.PetState
import com.pleosplayground.drivequest.core.decodePet
import com.pleosplayground.drivequest.core.encode
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(
    name = "drivequest",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)
private val PET_KEY = stringPreferencesKey("pet")

interface PetRepository {
    suspend fun load(): PetState
    suspend fun save(state: PetState)
}

class PetStore(context: Context) : PetRepository {
    private val store = context.applicationContext.dataStore

    override suspend fun load(): PetState = decodePet(store.data.first()[PET_KEY])

    override suspend fun save(state: PetState) {
        store.edit { it[PET_KEY] = state.encode() }
    }
}
