package com.pleosplayground.drivequest.data

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pleosplayground.drivequest.core.Hero
import com.pleosplayground.drivequest.core.decodeHero
import com.pleosplayground.drivequest.core.encode
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(
    name = "drivequest",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)
private val HERO_KEY = stringPreferencesKey("hero")

interface HeroRepository {
    suspend fun load(): Hero
    suspend fun save(hero: Hero)
}

class HeroStore(context: Context) : HeroRepository {
    private val store = context.applicationContext.dataStore

    override suspend fun load(): Hero = decodeHero(store.data.first()[HERO_KEY])

    override suspend fun save(hero: Hero) {
        store.edit { it[HERO_KEY] = hero.encode() }
    }
}
